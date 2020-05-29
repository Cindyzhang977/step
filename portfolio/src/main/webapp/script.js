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

import photosData from './photos-data.js';

/**
 * Create photo component with caption 
 */
function createPhoto(photo) {
    const component =
      `<div class="col-4"> \
          <figure class=\"figure\"> \
              <img src=${photo.src} class=\"figure-img img-fluid rounded\" alt=${photo.location}> \
              <div class=\"row figure-caption-container\"> \
                  <figcaption class=\"figure-caption photo-location\">${photo.location}</figcaption> \
                  <span class=\"dot\"></span> \
                  <figcaption class=\"figure-caption photo-date\">${photo.date}</figcaption> \
              </div> \
          </figure> \
      </div>`

  return component
}

/**
 * Map photos into gallery
 */
 function mapPhotos() {
     photosData.forEach(
        (photo) => {
            const component = createPhoto(photo);
            $('#gallery').append(component);
        } 
     )
 }

 mapPhotos();