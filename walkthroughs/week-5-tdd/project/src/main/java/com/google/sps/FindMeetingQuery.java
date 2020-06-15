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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Stack;

public final class FindMeetingQuery {

  /**
   * Return list of possible meeting times optimized to include as many optional attendees as possible in 
   * addition to mandatory attendees.  
   *
   * @param events list of events to consider for the attendees requested for the meeting in @param request
   * @return optimized list of meeting times where ranges accommodate the maximum number of optional attendees
   */
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    Collection<String> attendees = request.getAttendees();
    Collection<String> optionalAttendees = request.getOptionalAttendees();
    long duration = request.getDuration();

    List<TimeRange> unavailableTimes = getUnavailableTimes(events, attendees);
    List<TimeRange> unavailableTimesOptionalAttendees = getUnavailableTimes(events, optionalAttendees);
    List<TimeRange> meetingTimes = getMeetingTimes(unavailableTimes, duration);
    
    return getMaximallyAccommodatedMeetingTimes(meetingTimes, unavailableTimesOptionalAttendees, duration);
  }

  /** 
   * Return list of unavialable times to schedule the meeting. 
   * List of TimeRanges is in sorted order based on the comparator. 
   *
   * @param events collection of events to consider for @param attendees unavailable times
   * @param comparator the comparator function used to sort @return list of unavailable times
   */
  private List<TimeRange> getUnavailableTimes(Collection<Event> events, Collection<String> attendees) {
    List<TimeRange> unavailableTimesList = new ArrayList<>();
    for (Event e : events) {
      Collection<String> eventAttendees = e.getAttendees();
      if (!Collections.disjoint(eventAttendees, attendees)) {
        HashSet<String> intersection = new HashSet<>(eventAttendees);
        intersection.retainAll(attendees);
        for (int i = 0; i < intersection.size(); i++) {
          unavailableTimesList.add(e.getWhen());
        }
      }
    }

    Collections.sort(unavailableTimesList, TimeRange.ORDER_BY_START);
    return unavailableTimesList;
  }

  /**
   * Return list of possible meeting times based on the list of unavailable times and the duration of the desired meeting. 
   * 
   * @param unavailableTimes list of unavailable times where a meeting cannot be scheduled
   * @param duration length of the meeting
   * @return list of possible meeting times
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
   * Return an optimized version of possible meeting times that accommodates the maximum number of optional attendees. 
   *
   * @param availableMeetingTimes available meeting slots where only mandatory attendees are considered 
   * @param unavailableTimesOptionalAttendees unavailable times for optional attendees 
   * @param duration the duration of the meeting to be scheduled 
   * @return an optimized list of meeting times that would include the maximum number of attendees 
   */
  private List<TimeRange> getMaximallyAccommodatedMeetingTimes(List<TimeRange> availableMeetingTimes, List<TimeRange> unavailableTimesOptionalAttendees, long duration) {
    Stack<Interval> optimizedMeetingTimes = new Stack<>();
    int minUnvailable = Integer.MAX_VALUE;

    MeetingRanges meetingRanges = new MeetingRanges(availableMeetingTimes);
    for (TimeRange t : unavailableTimesOptionalAttendees) {
      meetingRanges.add(t);
    }

    Stack<Interval> meetingStack = meetingRanges.asStack();
    while (!meetingStack.empty()) {
      Interval interval = meetingStack.pop(); 
      TimeRange t = interval.getTimeRange();
      int intervalLength = t.duration();
      int numUnavailable = interval.getNumUnavailable();

      // push intervals when a meeting could be scheduled onto optimizedMeetingTimes 
      // update minUnavailable and clear optimizedMeetingTimes when a more optimal solution is found
      if (intervalLength >= duration && numUnavailable == minUnvailable) {
        optimizedMeetingTimes.push(interval);
      } else if (intervalLength >= duration && numUnavailable < minUnvailable) {
        optimizedMeetingTimes = new Stack<>();
        optimizedMeetingTimes.push(interval);
        minUnvailable = numUnavailable;
      }

      // append interval to an adjacent slot if it is shorter than duration and has less than or equal to the 
      // number of unavailable attendees as an adjacent slot 
      if (intervalLength < duration) {
        if (!optimizedMeetingTimes.empty()){
          Interval prev = optimizedMeetingTimes.peek();
          if (prev.end() == interval.start() && prev.getNumUnavailable() >= numUnavailable) {
            optimizedMeetingTimes.pop();
            optimizedMeetingTimes.push(new Interval(TimeRange.fromStartEnd(prev.start(), interval.end(), interval.end() == TimeRange.END_OF_DAY), prev.getNumUnavailable()));
          }
        }
        if (!meetingStack.empty()) {
          Interval next = meetingStack.peek();
          if (next.start() == interval.end() && next.getTimeRange().duration() < duration) {
            meetingStack.pop();
            meetingStack.push(new Interval(interval.start(), next.end(), Math.max(next.getNumUnavailable(), numUnavailable)));
          } else if (next.start() == interval.end() && next.getNumUnavailable() >= numUnavailable) {
            meetingStack.pop();
            meetingStack.push(new Interval(interval.start(), next.end(), next.getNumUnavailable()));
          }
        }
      }
    }

    // take timeranges from meetingStack to add the list of optimal meeting times
    List<TimeRange> optMeetingTimesList = new ArrayList<>();
    for (Interval interval : optimizedMeetingTimes) {
      optMeetingTimesList.add(interval.getTimeRange());
    }

    return optMeetingTimesList.size() == 0 ? availableMeetingTimes : optMeetingTimesList;
  }
}
