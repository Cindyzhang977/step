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
import photos_data from './photos-data.js'

/**
 * Create photo component with caption 
 */
function createPhoto(photo) {
    let component = [
        '<div id=' + photo.epoch + ' class="col-4">',
          '<figure class="figure">',
            '<img src=' + photo.src + ' class="figure-img img-fluid rounded" alt=' + photo.location + '>',
            '<div class="row figure-caption-container">',
              '<figcaption class="figure-caption photo-location">' + photo.location + '</figcaption>',
              '<span class="dot"></span>',
              '<figcaption class="figure-caption photo-date">' + photo.date + '</figcaption>',
            '</div>',
          '</figure>',
        '</div>'
    ]

  return $(component.join(''))
}

// dictionary mapping id : photo component
const photoComponents = {}

const monthToNum = {"Jan": 0, "Feb": 1, "Mar": 2, "Apr": 3, "May": 4, "Jun": 5,
                    "Jul": 6, "Aug": 7, "Sept": 8, "Oct": 9, "Nov": 10, "Dec": 11}

photos_data.forEach(
    (photo, index) => {
        // add index id to each photo
        photo.id = index

        // add epoch time to each photo
        let date = photo.date.split(" ")
        let year = date[2]
        let month = monthToNum[date[0]]
        let day = date[1].slice(0, -2)
        let epochTime = new Date(year, month, day).getTime() / 1000
        photo.epoch = epochTime

        // create photo component
        let component = createPhoto(photo)
        photoComponents[index] = component
    } 
)

/**
 * Map photos into gallery based on a filter if one is provided
 */
function mapPhotos(filter=null, components=null) {
  $('#gallery').empty()
  if (components != null) {
      components.forEach(
          (component) => $('#gallery').append(component) 
      )
  } else {
    photos_data.forEach(
      (photo) => { 
        if (filter == null || photo.tags.includes(filter)) {
            $('#gallery').append(photoComponents[photo.id])
        }
      } 
    )
    $("#newest").button('toggle')
    $("#oldest").button('dispose')
  }   
}

 /**
 * Order photos based on date
 */
function sortPhotos(order="newest") {
    let components = $('#gallery').contents().toArray()
    components.sort((component) => -component.id)
    mapPhotos(null, components)
}

// eventListeners for filtering photos based on an attribute
$("#filter-beach").click(() => mapPhotos("beach"))
$("#filter-sunset").click(() => mapPhotos("sunset"))
$("#filter-mountain").click(() => mapPhotos("mountain"))
$("#filter-none").click(() => mapPhotos())

// eventListeners for sorting photos
$("#newest").click(() => $("#newest").attr("class").includes("active") ? sortPhotos("newest") : null)
$("#oldest").click(() => $("#oldest").attr("class").includes("active") ? sortPhotos("oldest") : null)

window.onload = () => {
  mapPhotos()
  sortPhotos()
}