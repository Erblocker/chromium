// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.partnerbookmarks;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import org.chromium.chrome.browser.DisableHistogramsRule;
import org.chromium.testing.local.LocalRobolectricTestRunner;

/**
 * Unit tests for {@link PartnerBookmarksFaviconThrottle}.
 */
@RunWith(LocalRobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PartnerBookmarksFaviconThrottleTest {
    private static final String TEST_PREFERENCES_NAME = "partner_bookmarks_favicon_throttle_test";

    private PartnerBookmarksFaviconThrottle mFaviconThrottle;

    @Rule
    public DisableHistogramsRule mDisableHistogramsRule = new DisableHistogramsRule();

    @Before
    public void setUp() throws Exception {
        mFaviconThrottle = new PartnerBookmarksFaviconThrottle(
                RuntimeEnvironment.application, TEST_PREFERENCES_NAME);
    }

    @After
    public void tearDown() throws Exception {
        mFaviconThrottle.clearEntries();
    }

    @Test
    public void testInitEmpty() {
        Assert.assertEquals(mFaviconThrottle.numEntries(), 0);
    }

    @Test
    public void testCacheServerErrorFailures() {
        mFaviconThrottle.onFaviconFetched("url1", FaviconFetchResult.FAILURE_SERVER_ERROR);
        mFaviconThrottle.onFaviconFetched("url2", FaviconFetchResult.FAILURE_SERVER_ERROR);
        mFaviconThrottle.onFaviconFetched("url3", FaviconFetchResult.FAILURE_CONNECTION_ERROR);
        mFaviconThrottle.commit();

        mFaviconThrottle.init();
        Assert.assertFalse(mFaviconThrottle.shouldFetchFromServerIfNecessary("url1"));
        Assert.assertFalse(mFaviconThrottle.shouldFetchFromServerIfNecessary("url2"));
        Assert.assertTrue(mFaviconThrottle.shouldFetchFromServerIfNecessary("url3"));
    }

    @Test
    public void testOnlySuccessRemovesEntry() {
        mFaviconThrottle.onFaviconFetched("url1", FaviconFetchResult.FAILURE_SERVER_ERROR);
        mFaviconThrottle.commit();

        mFaviconThrottle.init();
        Assert.assertFalse(mFaviconThrottle.shouldFetchFromServerIfNecessary("url1"));
        mFaviconThrottle.onFaviconFetched(
                "url1", FaviconFetchResult.FAILURE_ICON_SERVICE_UNAVAILABLE);
        mFaviconThrottle.commit();

        mFaviconThrottle.init();
        Assert.assertFalse(mFaviconThrottle.shouldFetchFromServerIfNecessary("url1"));
        mFaviconThrottle.onFaviconFetched("url1", FaviconFetchResult.FAILURE_NOT_IN_CACHE);
        mFaviconThrottle.commit();

        mFaviconThrottle.init();
        Assert.assertFalse(mFaviconThrottle.shouldFetchFromServerIfNecessary("url1"));
        mFaviconThrottle.onFaviconFetched("url1", FaviconFetchResult.FAILURE_CONNECTION_ERROR);
        mFaviconThrottle.commit();

        mFaviconThrottle.init();
        Assert.assertFalse(mFaviconThrottle.shouldFetchFromServerIfNecessary("url1"));
        mFaviconThrottle.onFaviconFetched("url1", FaviconFetchResult.SUCCESS);
        mFaviconThrottle.commit();

        mFaviconThrottle.init();
        Assert.assertTrue(mFaviconThrottle.shouldFetchFromServerIfNecessary("url1"));
    }

    @Test
    public void testNewPartnerBookmarksRemovesOldEntries() {
        mFaviconThrottle.onFaviconFetched("url1", FaviconFetchResult.FAILURE_SERVER_ERROR);
        mFaviconThrottle.commit();

        mFaviconThrottle.init();
        Assert.assertEquals(mFaviconThrottle.numEntries(), 1);
        mFaviconThrottle.onFaviconFetched("url2", FaviconFetchResult.FAILURE_SERVER_ERROR);
        mFaviconThrottle.commit();

        mFaviconThrottle.init();
        Assert.assertEquals(mFaviconThrottle.numEntries(), 1);
        Assert.assertTrue(mFaviconThrottle.shouldFetchFromServerIfNecessary("url1"));
        Assert.assertFalse(mFaviconThrottle.shouldFetchFromServerIfNecessary("url2"));
    }

    @Test
    public void testShouldFetchFromServerIfNecessaryTrueIfNoPreviousEntry() {
        Assert.assertTrue(mFaviconThrottle.shouldFetchFromServerIfNecessary("unused_url"));
    }

    // TODO(thildebr): Test the code path for #shouldFetchFromServerIfNecessary where the timeout
    //                 has expired. This requires mocking out System.currentTimeMillis() somehow.

    @Test
    public void testShouldFetchFromServerIfNecessaryFalseIfNotExpired() {
        mFaviconThrottle.onFaviconFetched("url1", FaviconFetchResult.FAILURE_SERVER_ERROR);
        mFaviconThrottle.commit();

        mFaviconThrottle.init();
        Assert.assertFalse(mFaviconThrottle.shouldFetchFromServerIfNecessary("url1"));
    }
}
