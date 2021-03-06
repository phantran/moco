/*
 * Copyright (c) 2021. Tran Phan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.github.phantran.utils

import io.github.phantran.engine.Configuration
import io.github.phantran.persistence.ProjectMeta
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import java.io.File
import java.io.IOException
import java.lang.Exception
import kotlin.jvm.Throws
import kotlin.math.log

class GitProcessor(gitRootPath: String) {

    private val logger = MoCoLogger()
    private val builder = FileRepositoryBuilder()
    private val repo = builder.setGitDir(File("$gitRootPath/.git")).setMustExist(true).build()
    var headCommit: RevCommit = getHead(repo)
    var branch: String = repo.fullBranch

    @Throws(IOException::class)
    fun getChangedClsSinceLastStoredCommit(
        projectGroupID: String,
        projectMeta: MutableMap<String, String>
    ): List<String>? {
        // Return value meaning
        // 1. null -> latest stored commit id is not found
        // 2. empty list -> diff can be calculated, however there's no changed classes detected or no new commits
        // 3. list with elements -> changed classes detected
        var previousHead = projectMeta["storedHeadCommit"]!!
        if (!projectMeta["lastMavenSessionID"].isNullOrEmpty() &&
            projectMeta["lastMavenSessionID"] == Configuration.currentConfig?.mavenSession) {
            previousHead = projectMeta["storedPreviousHeadCommit"]!!
        }

        if (headCommit.name == previousHead) {
            return listOf()
        }
        Git(repo).use { git ->
            val commits = git.log().all().call()
            for (commit in commits) {
                if (commit.name == previousHead) {
                    return listDiff(repo, git, previousHead, headCommit.name, projectGroupID)
                }
            }
        }
        return null
    }

    @Throws(IOException::class)
    fun setHeadCommitMeta(projectMeta: ProjectMeta, gitMode: Boolean) {
        if (gitMode) {
            logger.debug("Last Maven session: ${projectMeta.meta["lastMavenSessionID"]}")
            logger.debug("Current Maven session: ${Configuration.currentConfig?.mavenSession}")
            if (projectMeta.meta["lastMavenSessionID"] != Configuration.currentConfig?.mavenSession) {
                if (projectMeta.meta["storedHeadCommit"].isNullOrEmpty()) {
                    projectMeta.meta["storedPreviousHeadCommit"] = ""
                } else {
                    projectMeta.meta["storedPreviousHeadCommit"] = projectMeta.meta["storedHeadCommit"]!!
                }
                logger.debug("Record the head commit ${headCommit.name} to project meta")
                projectMeta.meta["storedHeadCommit"] = headCommit.name
                projectMeta.meta["latestStoredBranchName"] = repo.fullBranch
            }
        } else {
            if (projectMeta.meta["storedHeadCommit"].isNullOrEmpty()) {
                projectMeta.meta["storedHeadCommit"] = ""
            }
            if (projectMeta.meta["storedPreviousHeadCommit"].isNullOrEmpty()) {
                projectMeta.meta["storedPreviousHeadCommit"] = ""
            }
            if (projectMeta.meta["latestStoredBranchName"].isNullOrEmpty()) {
                projectMeta.meta["latestStoredBranchName"] = ""
            }
        }
    }


    @Throws(IOException::class)
    private fun prepareTreeParser(repository: Repository, objectId: String): AbstractTreeIterator? {
        // from the commit we can build the tree which allows us to construct the TreeParser
        RevWalk(repository).use { walk ->
            val commit: RevCommit = walk.parseCommit(repository.resolve(objectId))
            val tree: RevTree = walk.parseTree(commit.tree.id)
            val treeParser = CanonicalTreeParser()
            repository.newObjectReader().use { reader -> treeParser.reset(reader, tree.id) }
            walk.dispose()
            return treeParser
        }
    }


    @Throws(GitAPIException::class, IOException::class)
    private fun listDiff(
        repository: Repository, git: Git,
        oldCommit: String, newCommit: String,
        groupId: String
    ): List<String> {
        val diff = git.diff()
            .setOldTree(prepareTreeParser(repository, oldCommit))
            .setNewTree(prepareTreeParser(repository, newCommit))
            .call()

        val classFiles = diff.map { it.newPath }.filter { it.endsWith(".java") }
        val res = mutableListOf<String>()
        classFiles.map {
            try {
                val startIndex = it.indexOf(groupId, 0)
                if (startIndex > -1) {
                    res.add(it.substring(startIndex, it.length - 5))
                }
            } catch (ex: Exception) {
                logger.error("Error while getting changed classes since last recorded GIT commit")
            }
        }
        return res
    }

    @Throws(IOException::class)
    fun getHead(repo: Repository): RevCommit {
        return getCommit(repo, Constants.HEAD)
    }

    @Throws(IOException::class)
    private fun getCommit(repo: Repository, hash: String): RevCommit {
        val walk = RevWalk(repo)
        val id = repo.resolve(hash)
        val commit = walk.parseCommit(id)
        walk.dispose()
        return commit
    }
}