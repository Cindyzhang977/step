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

public final class FindMeetingQuery {

  /** 
   * Return list of unavialable times to schedule the meeting. 
   * List of TimeRanges is in sorted order based on the comparator. 
   * @param events collection of events to consider for @param attendees unavailable times
   * @param comparator the comparator function used to sort @return list of unavailable times
   */
  private List<TimeRange> getUnavailableTimes(Collection<Event> events, Collection<String> attendees, Comparator<TimeRange> comparator) {
    HashSet<TimeRange> unavailableTimesSet = new HashSet<>();
    for (Event e : events) {
      Collection<String> eventAttendees = e.getAttendees();
      if (!Collections.disjoint(eventAttendees, attendees)) {
        unavailableTimesSet.add(e.getWhen());
      }
    }

    List<TimeRange> unavailableTimesList = new ArrayList<>(unavailableTimesSet);
    Collections.sort(unavailableTimesList, comparator);
    return unavailableTimesList;
  }

  /**
   * Return list of possible meeting times based on the list of unavailable times and the duration of the desired meeting. 
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
   * @param meetingTimes available meeting slots where only mandatory attendees are considered 
   * @param unavailableTimesOptionalAttendees unavailable times for optional attendees 
   * @param duration the duration of the meeting to be scheduled 
   * @return an optimized list of meeting times that would include the maximum number of attendees 
   */
  private List<TimeRange> getOptimiazedMeetingTimes(List<TimeRange> meetingTimes, List<TimeRange> unavailableTimesOptionalAttendees, long duration) {
    List<TimeRange> optimizedMeetingTimes = new ArrayList<>();
    
    for (int i = 0; i < meetingTimes.size(); i++) {
      TimeRange meetingSlot = meetingTimes.get(i);
      boolean canSkipRange = false;
      for (TimeRange t : unavailableTimesOptionalAttendees) {
        int start = t.start();
        int end = t.end();

        // meeting slot is not added to optimizedMeetingtimes if optional time is the same as meeting slot 
        // and there are other range options still available 
        int numSlotsUnconsidered = meetingTimes.size() - i - 1;
        if (t.equals(meetingSlot) && optimizedMeetingTimes.size() + numSlotsUnconsidered > 0) {
          canSkipRange = true;
          break;
        }

        // move on to next optional time range if current t is out of range of original meeting slot 
        if (!t.overlaps(meetingSlot) || t.contains(meetingSlot)) {
          continue;
        }

        // update possible time range
        TimeRange possibleTime = getPossibleTimeRange(meetingSlot, t, duration, optimizedMeetingTimes);
        meetingSlot = possibleTime != null ? possibleTime : meetingSlot;
        
      }
      // add possible time range to optimized meeting times
      if (!canSkipRange) {
        optimizedMeetingTimes.add(meetingSlot);
      } 
    }

    return optimizedMeetingTimes;
  }

  /**
   * Return a meeting TimeRange that accommodates the optionalTime slot into meetingSlot if possible. 
   * If two meeting TimeRanges are possible, at the first one to optimizedMeetingTimes 
   * MeetingSlot and optionalTime overlap, and meetingSlot is not a subrange of optionalTime. 
   * @param meetingSlot available meeting slot to schedule the meeting
   * @param optionalTime an optional attendee's unavailability to try to accommodate
   * @param duration length of meeting 
   * @return a new meeting TimeRange if @param optionalTime can be accommodated, else null  
   */
  private TimeRange getPossibleTimeRange(TimeRange meetingSlot, TimeRange optionalTime, long duration, List<TimeRange> optimizedMeetingTimes) {
    int meetingStart = meetingSlot.start();
    int meetingEnd = meetingSlot.end();
    int optionalStart = optionalTime.start();
    int optionalEnd = optionalTime.end();

    TimeRange option = null;
    if (optionalStart - meetingStart >= duration) {
      option = TimeRange.fromStartEnd(meetingStart, optionalStart, false);
    } 
    if (meetingEnd - optionalEnd >= duration) {
      if (option != null) {
        optimizedMeetingTimes.add(option);
      }
      option = TimeRange.fromStartEnd(optionalEnd, meetingEnd, meetingEnd == TimeRange.END_OF_DAY);
    }
    
    return option;
  }

  /**
   * Return list of possible meeting times optimized to include as many optional attendees as possible in 
   * addition to mandatory attendees.  
   * @param events list of events to consider for the attendees requested for the meeting in @param request
   * @return optimized list of meeting times where ranges accommodate the maximum number of optional attendees
   */
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    Collection<String> attendees = request.getAttendees();
    Collection<String> optionalAttendees = request.getOptionalAttendees();
    long duration = request.getDuration();

    List<TimeRange> unavailableTimes = getUnavailableTimes(events, attendees, TimeRange.ORDER_BY_START);
    List<TimeRange> unavailableTimesOptionalAttendees = getUnavailableTimes(events, optionalAttendees, TimeRange.ORDER_BY_END);
    
    List<TimeRange> meetingTimes = getMeetingTimes(unavailableTimes, duration);
    List<TimeRange> optimizedMeetingTimes = getOptimiazedMeetingTimes(meetingTimes, unavailableTimesOptionalAttendees, duration);

    return optimizedMeetingTimes;
  }
}
