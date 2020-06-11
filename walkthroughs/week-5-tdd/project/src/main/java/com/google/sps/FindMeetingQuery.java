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

  /** 
   * Return list of unavialable times to schedule the meeting. 
   * List of TimeRanges is in sorted order from earliest to latest start time.
   */
  private List<TimeRange> getUnavailableTimes(Collection<Event> events, Collection<String> attendees) {
    HashSet<TimeRange> unavailableTimesSet = new HashSet<>();
    for (Event e : events) {
      Collection<String> eventAttendees = e.getAttendees();
      if (!Collections.disjoint(eventAttendees, attendees)) {
        unavailableTimesSet.add(e.getWhen());
      }
    }

    List<TimeRange> unavailableTimesList = new ArrayList<>(unavailableTimesSet);
    Collections.sort(unavailableTimesList, TimeRange.ORDER_BY_START);
    return unavailableTimesList;
  }

  /**
   * Return list of possible meeting times based on the list of unavailable times and the duration of the desired meeting. 
   */
  private List<TimeRange> getMeetingTimes(List<TimeRange> unavailableTimes, long duration) {
    List<TimeRange> meetingTimes = new ArrayList<>();
    int firstAvail = TimeRange.START_OF_DAY;

    for (TimeRange t : unavailableTimes) {
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

  /**
   * Return list of possible meeting times for all attendees if possible, otherwise returns list of  
   * possible meeting times for only mandatory attendees.  
   */
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    Collection<String> attendees = request.getAttendees();
    Collection<String> optionalAttendees = request.getOptionalAttendees();
    Collection<String> allAttendees = new HashSet<>();
    allAttendees.addAll(attendees);
    allAttendees.addAll(optionalAttendees);
    long duration = request.getDuration();

    List<TimeRange> unavailableTimes = getUnavailableTimes(events, attendees);
    List<TimeRange> unavailableTimesWithAllAttendees = getUnavailableTimes(events, allAttendees);
    
    List<TimeRange> meetingTimes = getMeetingTimes(unavailableTimes, duration);
    List<TimeRange> meetingTimesWithAllAttendees = getMeetingTimes(unavailableTimesWithAllAttendees, duration);

    return meetingTimesWithAllAttendees.size() == 0 && attendees.size() != 0 ? meetingTimes : meetingTimesWithAllAttendees;
  }
}
