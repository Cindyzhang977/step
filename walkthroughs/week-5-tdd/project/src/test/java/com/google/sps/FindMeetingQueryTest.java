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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** */
@RunWith(JUnit4.class)
public final class FindMeetingQueryTest {
  private static final Collection<Event> NO_EVENTS = Collections.emptySet();
  private static final Collection<String> NO_ATTENDEES = Collections.emptySet();

  // Some people that we can use in our tests.
  private static final String PERSON_A = "Person A";
  private static final String PERSON_B = "Person B";
  private static final String PERSON_C = "Person C";
  private static final String PERSON_D = "Person D";
  private static final String PERSON_E = "Person E";

  // All dates are the first day of the year 2020.
  private static final int TIME_0800AM = TimeRange.getTimeInMinutes(8, 0);
  private static final int TIME_0830AM = TimeRange.getTimeInMinutes(8, 30);
  private static final int TIME_0845AM = TimeRange.getTimeInMinutes(8, 45);
  private static final int TIME_0900AM = TimeRange.getTimeInMinutes(9, 0);
  private static final int TIME_0930AM = TimeRange.getTimeInMinutes(9, 30);
  private static final int TIME_1000AM = TimeRange.getTimeInMinutes(10, 0);
  private static final int TIME_1100AM = TimeRange.getTimeInMinutes(11, 00);
  private static final int TIME_0300PM = TimeRange.getTimeInMinutes(15, 00);
  private static final int TIME_0500PM = TimeRange.getTimeInMinutes(17, 00);
  private static final int TIME_1000PM = TimeRange.getTimeInMinutes(22, 00);

  private static final int DURATION_30_MINUTES = 30;
  private static final int DURATION_60_MINUTES = 60;
  private static final int DURATION_90_MINUTES = 90;
  private static final int DURATION_1_HOUR = 60;
  private static final int DURATION_2_HOUR = 120;
  private static final int DURATION_4_HOUR = 240;

  private FindMeetingQuery query;

  @Before
  public void setUp() {
    query = new FindMeetingQuery();
  }

  @Test
  public void optionsForNoAttendees() {
    MeetingRequest request = new MeetingRequest(NO_ATTENDEES, DURATION_1_HOUR);

    Collection<TimeRange> actual = query.query(NO_EVENTS, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.WHOLE_DAY);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void noOptionsForTooLongOfARequest() {
    // The duration should be longer than a day. This means there should be no options.
    int duration = TimeRange.WHOLE_DAY.duration() + 1;
    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), duration);

    Collection<TimeRange> actual = query.query(NO_EVENTS, request);
    Collection<TimeRange> expected = Arrays.asList();

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void eventSplitsRestriction() {
    // The event should split the day into two options (before and after the event).
    Collection<Event> events = Arrays.asList(new Event("Event 1",
        TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES), Arrays.asList(PERSON_A)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            TimeRange.fromStartEnd(TIME_0900AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void everyAttendeeIsConsidered() {
    // Have each person have different events. We should see two options because each person has
    // split the restricted times.
    //
    // Events  :       |--A--|     |--B--|
    // Day     : |-----------------------------|
    // Options : |--1--|     |--2--|     |--3--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_0830AM, TIME_0900AM, false),
            TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void optionalAttendeeIsNotIncluded() {
    // Based on everyAttendeeIsConsidered, add an optional attendee C who has an all-day event. 
    // The same three time slots should be returned as when C was not invited.
    //
    // Events  :       |--A--|     |--B--|
    // Optional: |--------------C--------------|
    // Day     : |-----------------------------|
    // Options : |--1--|     |--2--|     |--3--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)),
        new Event("Event 3", TimeRange.WHOLE_DAY,
            Arrays.asList(PERSON_C)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_C);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_0830AM, TIME_0900AM, false),
            TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void optionalAttendeeIsIncluded() {
    // Based on everyAttendeeIsConsidered, add an optional attendee C who has an event between 8:30 and 9:00. 
    // Now only the early and late parts of the day should be returned.
    //
    // Events  :       |--A--|     |--B--|
    // Optional:             |--C--|
    // Day     : |-----------------------------|
    // Options : |--1--|                 |--3--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)),
        new Event("Event 3", TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_C)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_C);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void overlappingEvents() {
    // Have an event for each person, but have their events overlap. We should only see two options.
    //
    // Events  :       |--A--|
    //                     |--B--|
    // Day     : |---------------------|
    // Options : |--1--|         |--2--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0830AM, DURATION_60_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_60_MINUTES),
            Arrays.asList(PERSON_B)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            TimeRange.fromStartEnd(TIME_1000AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void nestedEvents() {
    // Have an event for each person, but have one person's event fully contain another's event. We
    // should see two options.
    //
    // Events  :       |----A----|
    //                   |--B--|
    // Day     : |---------------------|
    // Options : |--1--|         |--2--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0830AM, DURATION_90_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            TimeRange.fromStartEnd(TIME_1000AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void doubleBookedPeople() {
    // Have one person, but have them registered to attend two events at the same time.
    //
    // Events  :       |----A----|
    //                     |--A--|
    // Day     : |---------------------|
    // Options : |--1--|         |--2--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0830AM, DURATION_60_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void justEnoughRoom() {
    // Have one person, but make it so that there is just enough room at one point in the day to
    // have the meeting.
    //
    // Events  : |--A--|     |----A----|
    // Day     : |---------------------|
    // Options :       |-----|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartEnd(TIME_0900AM, TimeRange.END_OF_DAY, true),
            Arrays.asList(PERSON_A)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void justEnoughRoomWithoutOptionalAttendee() {
    // Based on justEnoughRoom, add an optional attendee B who has an event between 8:30 and 8:45. 
    // The optional attendee should be ignored since considering their schedule would result in a time slot smaller than the requested time.
    //
    // Events  : |--A--|     |----A----|
    // Optional:       |-B-|
    // Day     : |---------------------|
    // Options :       |-----|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartEnd(TIME_0900AM, TimeRange.END_OF_DAY, true),
            Arrays.asList(PERSON_A)),
        new Event("Event 3", TimeRange.fromStartEnd(TIME_0830AM, TIME_0845AM, false),
            Arrays.asList(PERSON_B)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_B);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void ignoresPeopleNotAttending() {
    // Add an event, but make the only attendee someone different from the person looking to book
    // a meeting. This event should not affect the booking.
    Collection<Event> events = Arrays.asList(new Event("Event 1",
        TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES), Arrays.asList(PERSON_A)));
    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_B), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.WHOLE_DAY);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void noConflicts() {
    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(NO_EVENTS, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.WHOLE_DAY);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void notEnoughRoom() {
    // Have one person, but make it so that there is not enough room at any point in the day to
    // have the meeting.
    //
    // Events  : |--A-----| |-----A----|
    // Day     : |---------------------|
    // Options :

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartEnd(TIME_0900AM, TimeRange.END_OF_DAY, true),
            Arrays.asList(PERSON_A)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_60_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = Arrays.asList();

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void optionalAttendeesOnlyWithRoom() {
    // No mandatory attendees, just two optional attendees with several gaps in their schedules. 
    // Those gaps should be identified and returned.
    //
    // Events  : |--A--|  |--B--|
    // Day     : |---------------------|
    // Options :       |--|     |------|
    Collection<Event> events = Arrays.asList(
          new Event("Event 1", TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
              Arrays.asList(PERSON_A)),
          new Event("Event 2", TimeRange.fromStartEnd(TIME_0900AM, TIME_1100AM, false),
              Arrays.asList(PERSON_B)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_A);
    request.addOptionalAttendee(PERSON_B);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TIME_0830AM, TIME_0900AM, false),
            TimeRange.fromStartEnd(TIME_1100AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Ignore
  @Test
  public void optionalAttendeesOnlyNoRoom() {
    // No mandatory attendees, just two optional attendees with no gaps in their schedules. 
    // Query should return that no time is available.
    //
    // Optional: |--A--|  
    //                |--------B-------|
    // Day     : |---------------------|
    // Options :     
    Collection<Event> events = Arrays.asList(
          new Event("Event 1", TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0900AM, false),
              Arrays.asList(PERSON_A)),
          new Event("Event 2", TimeRange.fromStartEnd(TIME_0845AM, TimeRange.END_OF_DAY, true),
              Arrays.asList(PERSON_B)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_A);
    request.addOptionalAttendee(PERSON_B);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = Arrays.asList();

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void optimizeOptionalAttendeesNoGap_IncludeTwoOfThree() {
    // Optimize to schedule around the most optional attendees possible.
    // Query should accommodate Person B and D but not Person C. 
    //
    // Events  : |--A--|  
    // Optional:      |---B---|     |-B|
    //                        |--C--|
    //                              |-D|
    // Day     : |---------------------|
    // Options :              |-----|   
    Collection<Event> events = Arrays.asList(
          new Event("Event 1", TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0900AM, false),
              Arrays.asList(PERSON_A)),
          new Event("Event 2", TimeRange.fromStartEnd(TIME_0845AM, TIME_0500PM, false),
              Arrays.asList(PERSON_B)),
          new Event("Event 3", TimeRange.fromStartEnd(TIME_1000PM, TimeRange.END_OF_DAY, true),
              Arrays.asList(PERSON_B, PERSON_D)),
          new Event("Event 4", TimeRange.fromStartEnd(TIME_0500PM, TIME_1000PM, false),
              Arrays.asList(PERSON_C)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_4_HOUR);
    request.addOptionalAttendee(PERSON_B);    
    request.addOptionalAttendee(PERSON_C);
    request.addOptionalAttendee(PERSON_D);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.fromStartEnd(TIME_0500PM, TIME_1000PM, false));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void optimizeOptionalAttendeesNotEnoughRoom_IncludeTwoOfThree() {
    // Optimize to schedule around the most optional attendees possible.
    // Query should accommodate Person C and B but there is not enough room to include Person D. 
    //
    // Events  : |--A--|        |-A-|
    // Optional:                    |-B|
    //                          |-C-|
    //               |---D---|
    // Day     : |---------------------|
    // Options :       |--------|  
    Collection<Event> events = Arrays.asList(
          new Event("Event 1", TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0900AM, false),
              Arrays.asList(PERSON_A)),
          new Event("Event 2", TimeRange.fromStartEnd(TIME_0500PM, TIME_1000PM, false),
              Arrays.asList(PERSON_A, PERSON_C)),
          new Event("Event 3", TimeRange.fromStartEnd(TIME_1000PM, TimeRange.END_OF_DAY, true),
              Arrays.asList(PERSON_B)),
          new Event("Event 4", TimeRange.fromStartEnd(TIME_0830AM, TIME_0300PM, false),
              Arrays.asList(PERSON_D)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_4_HOUR);
    request.addOptionalAttendee(PERSON_B);    
    request.addOptionalAttendee(PERSON_C);
    request.addOptionalAttendee(PERSON_D);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.fromStartEnd(TIME_0900AM, TIME_0500PM, false));

    Assert.assertEquals(expected, actual);   
  }

   @Test
  public void optimizeOptionalAttendees_NoChangeFromMandatory() {
    // Optimize to schedule around the most optional attendees possible.
    // Query should accommodate all optional attendees. 
    //
    // Events  : |--A--|        |-A-|
    // Optional:                   |-C-|
    //           |-B-|
    // Day     : |---------------------|
    // Options :       |--------|  
    Collection<Event> events = Arrays.asList(
          new Event("Event 1", TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0900AM, false),
              Arrays.asList(PERSON_A)),
          new Event("Event 2", TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
              Arrays.asList(PERSON_B)),
          new Event("Event 3", TimeRange.fromStartEnd(TIME_0500PM, TIME_1000PM, false),
              Arrays.asList(PERSON_A, PERSON_C)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_4_HOUR);
    request.addOptionalAttendee(PERSON_B);    
    request.addOptionalAttendee(PERSON_C);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.fromStartEnd(TIME_0900AM, TIME_0500PM, false));

    Assert.assertEquals(expected, actual); 
  }

  @Test
  public void oneOptimalSolution() {
    // Query should return only one time range that accommodates two/three optional attendees and all mandatory attendees
    // Events  :     |-A-|   |-B-|
    // Optional:         |-C-|   |-C-|
    //           |-D-|   |-D-|
    //           |-E-|
    // Day     : |-------------------|
    // Options :                 |-1-|
    Collection<Event> events = Arrays.asList(
          new Event("Event 1", TimeRange.fromStartEnd(TIME_0800AM, TIME_0900AM, false),
              Arrays.asList(PERSON_A)),
          new Event("Event 2", TimeRange.fromStartEnd(TIME_0300PM, TIME_0500PM, false),
              Arrays.asList(PERSON_B)),
          new Event("Event 3", TimeRange.fromStartEnd(TIME_0900AM, TIME_0300PM, false),
              Arrays.asList(PERSON_C, PERSON_D)),
          new Event("Event 4", TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
              Arrays.asList(PERSON_D, PERSON_E)),
          new Event("Event 5", TimeRange.fromStartEnd(TIME_0500PM, TimeRange.END_OF_DAY, true),
              Arrays.asList(PERSON_C)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_2_HOUR);
    request.addOptionalAttendee(PERSON_C);    
    request.addOptionalAttendee(PERSON_D);
    request.addOptionalAttendee(PERSON_E);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.fromStartEnd(TIME_0500PM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual); 
  }

  @Test
  public void oneOptimalSolution_threeSlots() {
    // Query should return three time ranges that accommodate two/three optional attendees and all mandatory attendees
    // Events  :     |-A-|   |-B-|
    // Optional:         |-C-|   |C|
    //                |-D-|     
    //           |--E--|         
    // Day     : |-------------------|
    // Options : |---|    |--|   |---|
    Collection<Event> events = Arrays.asList(
          new Event("Event 1", TimeRange.fromStartEnd(TIME_0800AM, TIME_0900AM, false),
              Arrays.asList(PERSON_A)),
          new Event("Event 2", TimeRange.fromStartEnd(TIME_0300PM, TIME_0500PM, false),
              Arrays.asList(PERSON_B)),
          new Event("Event 3", TimeRange.fromStartEnd(TIME_0900AM, TIME_0300PM, false),
              Arrays.asList(PERSON_C)),
          new Event("Event 4", TimeRange.fromStartEnd(TIME_0500PM, TIME_1000PM, false),
              Arrays.asList(PERSON_C)),
          new Event("Event 5", TimeRange.fromStartEnd(TIME_0830AM, TIME_1000AM, false),
              Arrays.asList(PERSON_D)),
          new Event("Event 6", TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0845AM, true),
              Arrays.asList(PERSON_E)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_2_HOUR);
    request.addOptionalAttendee(PERSON_C);    
    request.addOptionalAttendee(PERSON_D);
    request.addOptionalAttendee(PERSON_E);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = 
        Arrays.asList(
            TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_1000AM, TIME_0300PM, false),
            TimeRange.fromStartEnd(TIME_1000PM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual); 
  }

  @Test
  public void optimizeReplacingRanges() {
    // Optional attendee unavailability overlap with mandatory attendee availability perfectly. 
    // Query should return every slot.
    //
    // Events  :       |--A--|     |--B--|
    // Optional: |--C--|     |--C--|     
    //                                   |--D--|
    // Day     : |-----------------------------|
    // Options : |-----|     |-----|     |-----|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)),
        new Event("Event 3", TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            Arrays.asList(PERSON_C)),
        new Event("Event 4", TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_C)),
        new Event("Event 5", TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true),
            Arrays.asList(PERSON_D)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_C);
    request.addOptionalAttendee(PERSON_D);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = 
        Arrays.asList(
            TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_0830AM, TIME_0900AM, false),
            TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void optimizeFewestOverlaps() {
    // Optional attendee unavailability overlap with mandatory attendee availability perfectly. 
    // Query should return the first two with fewest overlaps.
    //
    // Events  :       |--A--|     |--B--|
    // Optional: |--C--|     |--C--|     
    //                                   |--D--|
    //                                   |--E--|
    // Day     : |-----------------------------|
    // Options : |-----|     |-----|    

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)),
        new Event("Event 3", TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            Arrays.asList(PERSON_C)),
        new Event("Event 4", TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_C)),
        new Event("Event 5", TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true),
            Arrays.asList(PERSON_D, PERSON_E)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_C);
    request.addOptionalAttendee(PERSON_D);
    request.addOptionalAttendee(PERSON_E);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = 
        Arrays.asList(
            TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_0830AM, TIME_0900AM, false));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void optimizeFewestOverlaps2() {
    // Optional attendee unavailability overlap with mandatory attendee availability almost perfectly. 
    // Query should return every slot with one optional attendee unavailable.
    //
    // Events  :       |--A--|     |--B--|
    // Optional: |--C--|     |--C--|     
    //                                   |--D--|
    //                                   |-E-|
    // Day     : |-----------------------------|
    // Options : |-----|     |-----|         |-|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)),
        new Event("Event 3", TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            Arrays.asList(PERSON_C)),
        new Event("Event 4", TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_C)),
        new Event("Event 5", TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true),
            Arrays.asList(PERSON_D)),
        new Event("Event 6", TimeRange.fromStartEnd(TIME_0930AM, TIME_1100AM, false),
            Arrays.asList(PERSON_E)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_C);
    request.addOptionalAttendee(PERSON_D);
    request.addOptionalAttendee(PERSON_E);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = 
        Arrays.asList(
            TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_0830AM, TIME_0900AM, false),
            TimeRange.fromStartEnd(TIME_1100AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void optimizeIncludeEveryone() {
    // Everyone can be included in the meeting.
    //
    // Events  :       |--A--|     |--B--|
    // Optional:   |--C--|   |--C--|     
    //                                   |--D--|
    // Day     : |-----------------------------|
    // Options : |-|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_1000AM, DURATION_1_HOUR),
            Arrays.asList(PERSON_B)),
        new Event("Event 3", TimeRange.fromStartEnd(TIME_0800AM, TIME_0930AM, false),
            Arrays.asList(PERSON_C)),
        new Event("Event 4", TimeRange.fromStartDuration(TIME_0930AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_C)),
        new Event("Event 5", TimeRange.fromStartEnd(TIME_1100AM, TimeRange.END_OF_DAY, true),
            Arrays.asList(PERSON_D)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_C);
    request.addOptionalAttendee(PERSON_D);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false));

    Assert.assertEquals(expected, actual);
  }
}
