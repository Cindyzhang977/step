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

import { photosData } from "./photos-data.js";

/**
 * Create photo component with caption
 * @param {Photo object} photo a Photo object that contains the data for a photo html component
 * @return {html component}
 */
function createPhoto(photo) {
  let component = [
    "<div id=" + photo.epoch + ' class="col-lg-4 col-md-6 col-sm-12">',
    '<figure class="figure">',
    '<div class="figure-img-container">',
    "<img src=" +
      photo.src +
      ' class="figure-img img-fluid rounded" alt=' +
      photo.location +
      ">",
    "</div>",
    '<div class="row figure-caption-container">',
    '<figcaption class="figure-caption photo-location">' +
      photo.location +
      "</figcaption>",
    '<span class="dot"></span>',
    '<figcaption class="figure-caption photo-date">' +
      photo.date +
      "</figcaption>",
    "</div>",
    "</figure>",
    "</div>",
  ];
  return $(component.join(""));
}

// dictionary mapping id : photo component
const photoComponents = {};

const allComponents = [];

const monthToNum = {
  Jan: 0,
  Feb: 1,
  Mar: 2,
  Apr: 3,
  May: 4,
  Jun: 5,
  Jul: 6,
  Aug: 7,
  Sept: 8,
  Oct: 9,
  Nov: 10,
  Dec: 11,
};

for (let index = 0; index < photosData.length; index++) {
  let photo = photosData[index];

  // add index id to each photo
  photo.id = index;

  // add epoch time to each photo
  let date = photo.date.split(" ");
  let year = date[2];
  let month = monthToNum[date[0]];
  let day = date[1].slice(0, -2);
  let epochTime = new Date(year, month, day).getTime() / 1000;
  photo.epoch = epochTime;

  // create photo component
  let component = createPhoto(photo);
  photoComponents[index] = component;
  allComponents.push(component);
}

/**
 * Map photos into gallery
 * @param {string} components the list of specific photo components to display
 */
function mapPhotos(components = allComponents) {
  $("#gallery").empty();
  for (const component of components) {
    $("#gallery").append(component);
  }
  $("#newest").button("toggle");
  $("#oldest").button("dispose");
}

/**
 * Order photos based on date
 * @param order the prefered ording to display photos (newest first vs oldest first)
 */
function sortPhotos(order = "newest") {
  let components = $("#gallery").contents().toArray();
  if (order == "oldest") {
    components.sort((a, b) => a.id - b.id);
  } else {
    components.sort((a, b) => b.id - a.id);
  }
  mapPhotos(components);
}

/**
 * Filter photos to determine which ones to display
 * @param {string} filter the applied filter used to pick which photos to display
 */
function filterPhotos(filter = null) {
  let components = [];
  for (const photo of photosData) {
    if (filter == null || photo.tags.includes(filter)) {
      components.push(photoComponents[photo.id]);
    }
  }
  mapPhotos(components);
}

// eventListeners for filtering photos based on an attribute
$("#filter-beach").click(() => filterPhotos("beach"));
$("#filter-sunset").click(() => filterPhotos("sunset"));
$("#filter-mountain").click(() => filterPhotos("mountain"));
$("#filter-none").click(() => filterPhotos());

// eventListeners for sorting photos
$("#newest").click(() =>
  $("#newest").attr("class").includes("active") ? sortPhotos("newest") : null
);
$("#oldest").click(() =>
  $("#oldest").attr("class").includes("active") ? sortPhotos("oldest") : null
);

window.onload = () => {
  mapPhotos();
  sortPhotos();
};
