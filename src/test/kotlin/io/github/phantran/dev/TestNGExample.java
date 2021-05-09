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

package io.github.phantran.dev;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.Assert;

public class TestNGExample {

    @BeforeMethod
    public void beforeMethod() {
    }

    @Test
    public void LoginTest()
    {
        Feature a = new Feature(25, 123);
        a.doNothing();
        a.foo(3);
        Assert.assertEquals(1, 1);
    }

    @Test
    public void LogoutTest()
    {
        Assert.assertEquals(5, 5);

    }

    @AfterMethod
    public void afterMethod() {
    }
}