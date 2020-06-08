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

package com.google.sps.data;

import javax.annotation.Nullable;

/**
 * Class representing the a destination recommendation comment.
 * Contains location and link.
 */
public class Comment {
  private String location;
  private String link;
  private String description;
  private String id;
  private String userEmail;
  private String displayedName;

  public Comment(String location, String link, @Nullable String description, String id, String userEmail, String displayedName) {
    this.location = location;
    this.link = link;
    this.description = description;
    this.id = id;
    this.userEmail = userEmail;
    this.displayedName = displayedName;

    if (this.description != null) {
      this.description = this.description.trim();
    }
  }

  public String getLocation() {
    return this.location;
  }

  public String getLink() {
    return this.link;
  }

  public String getDescription() {
    return this.description;
  }

  public String getId() {
    return this.id;
  }

  public String getUserEmail() {
    return this.userEmail;
  }

  public String getDisplayedName() {
    return this.displayedName;
  }
}