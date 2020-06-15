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
import java.util.Iterator;
import java.util.List;
import java.util.HashSet;

/**
 * Class representing a collection of Intervals where a meeting could be scheduled. 
 */
public class MeetingRanges {

  private static HashSet<Interval> intervals = new HashSet<>();

  /**
   * Add the intervals that indicate possible meeting times to initiate MeetingRanges.
   */
  public MeetingRanges(List<TimeRange> meetingRanges) {
    for (TimeRange t : meetingRanges) {
      intervals.add(new Interval(t, 1));
    }
  }

  /**
   * Add an interval to accomodate into intervals. Only keep range that is in bounds with possible meeting times. 
   * Update numOccurrence for overlaps. 
   */
  public void add(TimeRange timerange) {
    Iterator<Interval> intervalIter = intervals.iterator();
    while intervalIter.hasNext() {
      Interval i = intervalIter.next();
      TimeRange t = i.getTimeRange();
      
      if (t.overlaps(timerange)) {
        intervals.remove(i);

        if (t.equals(timerange)) {
          intervals.add(new Interval(t, i.getNumOccurrences() + 1));
        } else if (!timerange.contains(t)) {
          int timerangeStart = timerange.start();
          int timerangeEnd = timerange.end();
          int tStart = t.start();
          int tEnd = t.end();

          if (t.contains(timerange)) {
            // |-------- t -------|
            //   |- timerange -|
            intervals.add(TimeRange.fromStartEnd(tStart, timerangeStart, false), i.getNumOccurrences());
            intervals.add(TimeRange.fromStartEnd(timerangeEnd, tEnd, tEnd == TimeRange.END_OF_DAY), i.getNumOccurrences());
            intervals.add(timerange, i.getNumOccurrences() + 1);
          } else if (timerangeStart >= tStart && timerangeEnd > tEnd) {
            // |----- t ----|
            //      |-- timerange --|
            intervals.add(TimeRange.fromStartEnd(timerangeStart, tEnd, false), i.getNumOccurrences() + 1);
            intervals.add(TimeRange.fromStartEnd(tStart, timerangeStart, false), i.getNumOccurrences());
          } else if (timerangeStart < tStart && timerangeEnd <= tEnd) {
            //         |--- t ---|
            // |-- timerange --|
            intervals.add(TimeRange.fromStartEnd(tStart, timerangeEnd, false), i.getNumOccurrences() + 1);
            intervals.add(TimeRange.fromStartEnd(timerangeEnd, tEnd, tEnd == TimeRange.END_OF_DAY), i.getNumOccurrences());
          }
        }
      }
    }
  }

  /**
   * Return list of meeting intervals ordered by earliest start time.  
   */
  public List<Interval> asList() {
    List<Interval> = new ArrayList<>(intervals);
    Collections.sort(intervalsList, Interval.ORDER_BY_START);
    return intervalsList;
  }
}