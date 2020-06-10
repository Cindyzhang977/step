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
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public final class FindMeetingQuery {
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    Collection<String> attendees = request.getAttendees();
    long duration = request.getDuration();

    // create collection of unavailable times 
    HashSet<TimeRange> unavailableTimesSet = new HashSet<>();
    for (Event e : events) {
      Collection<String> eventAttendees = e.getAttendees();
      if (!Collections.disjoint(eventAttendees, attendees)) {
        unavailableTimesSet.add(e.getWhen());
      }
    }

    List<TimeRange> unavailableTimesList = new ArrayList<>(unavailableTimesSet);
    Collections.sort(unavailableTimesList, TimeRange.ORDER_BY_START);
    List<TimeRange> meetingTimes = new ArrayList<>();

    // search for possible meeting times 
    int firstAvail = TimeRange.START_OF_DAY;
    for (TimeRange t : unavailableTimesList) {
      int start = t.start();
      int end = t.end();
      if (start - firstAvail >= duration) {
        meetingTimes.add(TimeRange.fromStartEnd(firstAvail, start, false));
      }
      firstAvail = Math.max(end, firstAvail);
    }

    if (TimeRange.END_OF_DAY - firstAvail >= duration) {
      meetingTimes.add(TimeRange.fromStartEnd(firstAvail, TimeRange.END_OF_DAY, true));
    }

    return meetingTimes;
  }
}
