// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import java.util.Stack;

/**
 * Class representing a collection of Intervals where a meeting could be scheduled. 
 */
public class MeetingRanges {

  private static HashSet<Interval> intervals;

  /**
   * Add the intervals that indicate possible meeting times for a meeting to be scheduled, initiate MeetingRanges.
   */
  public MeetingRanges(List<TimeRange> meetingRanges) {
    intervals = new HashSet<>();
    for (TimeRange t : meetingRanges) {
      intervals.add(new Interval(t, 0));
    }
  }

  /**
   * Add the interval of @param timerange into intervals. Only keep intervals that are in bounds with possible meeting times. 
   * Update numAvailable for overlaps. 
   */
  public void add(TimeRange timerange) {
    HashSet<Interval> intervalsCopy = (HashSet) intervals.clone();
    for (Interval interval : intervalsCopy) {
      TimeRange t = interval.getTimeRange();
      
      if (t.overlaps(timerange)) {
        intervals.remove(interval);

        if (t.equals(timerange)) {
          intervals.add(new Interval(t, interval.getNumUnavailable() + 1));
        } else {
          int timerangeStart = timerange.start();
          int timerangeEnd = timerange.end();
          int tStart = t.start();
          int tEnd = t.end();

          if (t.contains(timerange)) {
            // |---------- t ---------|
            //     |- timerange -|
            intervals.add(new Interval(TimeRange.fromStartEnd(tStart, timerangeStart, false), interval.getNumUnavailable()));
            intervals.add(new Interval(TimeRange.fromStartEnd(timerangeEnd, tEnd, tEnd == TimeRange.END_OF_DAY), interval.getNumUnavailable()));
            intervals.add(new Interval(timerange, interval.getNumUnavailable() + 1));
          } else if (timerangeStart > tStart && timerangeEnd >= tEnd) {
            // |----- t ----|
            //      |-- timerange --|
            intervals.add(new Interval(TimeRange.fromStartEnd(timerangeStart, tEnd, tEnd == TimeRange.END_OF_DAY), interval.getNumUnavailable() + 1));
            intervals.add(new Interval(TimeRange.fromStartEnd(tStart, timerangeStart, false), interval.getNumUnavailable()));
          } else if (timerangeStart <= tStart && timerangeEnd < tEnd) {
            //         |--- t ---|
            // |-- timerange --|
            intervals.add(new Interval(TimeRange.fromStartEnd(tStart, timerangeEnd, timerangeEnd == TimeRange.END_OF_DAY), interval.getNumUnavailable() + 1));
            intervals.add(new Interval(TimeRange.fromStartEnd(timerangeEnd, tEnd, tEnd == TimeRange.END_OF_DAY), interval.getNumUnavailable()));
          } else {
            //    |-- t--|
            // |-- timerange --|
            intervals.add(new Interval(t, interval.getNumUnavailable() + 1));
          }
        }
      }
    }
  }

  /**
   * @return list of meeting intervals ordered by latest start time.  
   */
  public List<Interval> asList() {
    List<Interval> intervalsList = new ArrayList<>(intervals);
    Collections.sort(intervalsList, Interval.ORDER_BY_LATEST_START);
    return intervalsList;
  }

  /**
   * @return stack of meeting intervals where earliest start time is on top.  
   */
  public Stack<Interval> asStack() {
    Stack<Interval> meetingStack = new Stack<>();
    meetingStack.addAll(this.asList());
    return meetingStack;
  }
}