/*
 * The MIT License
 *
 * Copyright (c) 2010, InfraDNA, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.sauce_ondemand;

import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestAction;
import hudson.tasks.junit.TestObject;
import hudson.tasks.junit.TestResultAction.Data;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Kohsuke Kawaguchi
 */
public class VideoFactory extends Data {
    @Override
    public List<TestAction> getTestAction(TestObject testObject) {
        if (testObject instanceof CaseResult) {
            CaseResult cr = (CaseResult) testObject;
            String id = findSessionID(cr);
            return Collections.<TestAction>singletonList(new SauceOnDemandReport(id));
        }
        return Collections.emptyList();
    }

    static String findSessionID(CaseResult cr) {
        String id = findSessionID(cr.getStdout());
        if (id==null)   id = findSessionID(cr.getStderr());
        return id;
    }

    static String findSessionID(SuiteResult sr) {
        String id = findSessionID(sr.getStdout());
        if (id==null)   id = findSessionID(sr.getStderr());
        return id;
    }

    private static String findSessionID(String s) {
        if (s == null)  return null;

        Matcher m = SESSION_ID.matcher(s);
        if (m.find())
            return m.group(1);
        return null;
    }

    private static final Pattern SESSION_ID = Pattern.compile("SauceOnDemandSessionID=([0-9a-fA-F]+)");
}
