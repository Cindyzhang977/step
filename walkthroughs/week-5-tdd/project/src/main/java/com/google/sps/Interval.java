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

import java.util.Comparator;

/**
  * Class encapsulating a TimeRange and the number of attendees unavailable during that time.
  */
public class Interval {
  private int start;
  private int end;
  private int numUnavailable;
  private TimeRange timerange; 

  /**
   * A comparator for sorting intervals by their start time in ascending order.
   */
  public static final Comparator<Interval> ORDER_BY_START = new Comparator<Interval>() {
    @Override
    public int compare(Interval a, Interval b) {
      return Long.compare(a.start, b.start);
    }
  };

  public Interval(TimeRange timerange, int numUnavailable) {
    this.start = timerange.start();
    this.end = timerange.end();
    this.timerange = timerange;
    this.numUnavailable = numUnavailable;
  }

  public int getNumUnavailable() {
    return this.numUnavailable;
  }

  public TimeRange getTimeRange() {
    return this.timerange;
  }
}