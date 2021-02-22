package io.moco.engine.preprocessing

import io.moco.engine.Configuration
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import java.io.File
import java.io.IOException
import kotlin.jvm.Throws
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.api.errors.GitAPIException
import java.lang.Exception


object PreprocessingFilterByGit {

    @JvmStatic
    @Throws(IOException::class)
    fun getChangedClassesSinceLastStoredCommit(lastCommitName: String,
                                               gitRootPath: String? = Configuration.baseDir,
                                               projectArtifactID: String): List<String> {


        val builder = FileRepositoryBuilder()
        val repo = builder.setGitDir(File("$gitRootPath/.git")).setMustExist(true).build()
        val headCommit = getHead(repo)
        Git(repo).use { git ->
            val commits = git.log().all().call()
            for (commit in commits) {
                if (commit.name == lastCommitName) {
                    return listDiff(repo, git, lastCommitName, headCommit.name, projectArtifactID)
                }
            }
        }
        return listOf()
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
    private fun listDiff(repository: Repository, git: Git,
                         oldCommit: String, newCommit: String,
                         artifactID: String) : List<String> {

        val diff = git.diff()
            .setOldTree(prepareTreeParser(repository, oldCommit))
            .setNewTree(prepareTreeParser(repository, newCommit))
            .call()

        val classFiles = diff.map { it.newPath }.filter { it.endsWith(".java") }.
                    map { it.replace("/", ".") }
        val res = mutableListOf<String>()
        classFiles.map {
            try {
                val startIndex = it.indexOf(artifactID, 0)
                if (startIndex > -1) {
                    res.add(it.substring(startIndex, it.length - 5))
                }
            } catch (ex: Exception) {
                println("Error while getting changed classes between since stored git commit")
            }
        }
        return res
    }

    @JvmStatic
    @Throws(IOException::class)
    fun getHead(repo: Repository): RevCommit {
        return getCommit(repo, Constants.HEAD)
    }

        @JvmStatic
    @Throws(IOException::class)
    private fun getCommit(repo: Repository, hash: String): RevCommit {
        val walk = RevWalk(repo)
        val id = repo.resolve(hash)
        val commit = walk.parseCommit(id)
        walk.dispose()
        return commit
    }

}
