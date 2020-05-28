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

// photos data
import photos_data from './photos-data.js';

/**
 * Create photo component with caption 
 */
function createPhoto(photo) {
    let component = [
        '<div class="col-4">',
            '<figure class="figure">',
                '<img src=' + photo.src + ' class="figure-img img-fluid rounded" alt=' + photo.location + '>',
                '<div class="row figure-caption-container">',
                    '<figcaption class="figure-caption photo-location">' + photo.location + '</figcaption>',
                    '<span class="dot"></span>',
                    '<figcaption class="figure-caption photo-date">' + photo.date + '</figcaption>',
                '</div>',
            '</figure>',
        '</div>'
    ];

  return $(component.join(''));
}

const photoComponents = {}  //dictionary mapping id : photo component
photos_data.forEach(
    (photo, index) => {
        photo.id = index;
        photoComponents[index] = createPhoto(photo);
    } 
)

//TODO: map over photos_data, filter with tags, query photoComponents with id 

/**
 * Map photos into gallery
 */
 function mapPhotos(filter=null) {
     photo_data.forEach(
        (photo) => { 
            if (filter && filter in photo.tags)
                $('#gallery').append(component) 
        } 
     )
 }

 mapPhotos();