/*
 * Copyright (C) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.cloud.dataflow.sdk.util;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import com.google.cloud.dataflow.sdk.coders.Coder;
import com.google.cloud.dataflow.sdk.testing.CoderProperties;
import com.google.cloud.dataflow.sdk.transforms.windowing.GlobalWindow;
import com.google.cloud.dataflow.sdk.transforms.windowing.IntervalWindow;
import com.google.cloud.dataflow.sdk.util.TimerInternals.TimerData;
import com.google.cloud.dataflow.sdk.util.TimerInternals.TimerDataCoder;
import com.google.cloud.dataflow.sdk.util.state.StateNamespace;
import com.google.cloud.dataflow.sdk.util.state.StateNamespaces;

import org.joda.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link TimerInternals}.
 */
@RunWith(JUnit4.class)
public class TimerInternalsTest {

  @Test
  public void testTimerDataCoder() throws Exception {
    CoderProperties.coderDecodeEncodeEqual(
        TimerDataCoder.of(GlobalWindow.Coder.INSTANCE),
        TimerData.of(StateNamespaces.global(), new Instant(0), TimeDomain.EVENT_TIME));

    Coder<IntervalWindow> windowCoder = IntervalWindow.getCoder();
    CoderProperties.coderDecodeEncodeEqual(
        TimerDataCoder.of(windowCoder),
        TimerData.of(
            StateNamespaces.window(
                windowCoder, new IntervalWindow(new Instant(0), new Instant(100))),
            new Instant(99), TimeDomain.PROCESSING_TIME));
  }

  @Test
  public void testCompareTo() {
    Instant firstTimestamp = new Instant(100);
    Instant secondTimestamp = new Instant(200);
    IntervalWindow firstWindow = new IntervalWindow(new Instant(0), firstTimestamp);
    IntervalWindow secondWindow = new IntervalWindow(firstTimestamp, secondTimestamp);
    Coder<IntervalWindow> windowCoder = IntervalWindow.getCoder();

    StateNamespace firstWindowNs = StateNamespaces.window(windowCoder, firstWindow);
    StateNamespace secondWindowNs = StateNamespaces.window(windowCoder, secondWindow);

    TimerData firstEventTime = TimerData.of(firstWindowNs, firstTimestamp, TimeDomain.EVENT_TIME);
    TimerData secondEventTime = TimerData.of(firstWindowNs, secondTimestamp, TimeDomain.EVENT_TIME);
    TimerData thirdEventTime = TimerData.of(secondWindowNs, secondTimestamp, TimeDomain.EVENT_TIME);

    TimerData firstProcTime =
        TimerData.of(firstWindowNs, firstTimestamp, TimeDomain.PROCESSING_TIME);
    TimerData secondProcTime =
        TimerData.of(firstWindowNs, secondTimestamp, TimeDomain.PROCESSING_TIME);
    TimerData thirdProcTime =
        TimerData.of(secondWindowNs, secondTimestamp, TimeDomain.PROCESSING_TIME);

    assertThat(firstEventTime,
        comparesEqualTo(TimerData.of(firstWindowNs, firstTimestamp, TimeDomain.EVENT_TIME)));
    assertThat(firstEventTime, lessThan(secondEventTime));
    assertThat(secondEventTime, lessThan(thirdEventTime));
    assertThat(firstEventTime, lessThan(thirdEventTime));

    assertThat(secondProcTime,
        comparesEqualTo(TimerData.of(firstWindowNs, secondTimestamp, TimeDomain.PROCESSING_TIME)));
    assertThat(firstProcTime, lessThan(secondProcTime));
    assertThat(secondProcTime, lessThan(thirdProcTime));
    assertThat(firstProcTime, lessThan(thirdProcTime));

    assertThat(firstEventTime, not(comparesEqualTo(firstProcTime)));
    assertThat(firstProcTime,
        not(comparesEqualTo(TimerData.of(firstWindowNs,
            firstTimestamp,
            TimeDomain.SYNCHRONIZED_PROCESSING_TIME))));
  }
}

