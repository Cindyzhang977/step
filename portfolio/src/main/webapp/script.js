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

import { photosData } from './photos-data.js';

/**
 * Create photo component with caption
 * @param {Photo object} photo a Photo object that contains the data for a photo html component
 * @return {html component}
 */
function createPhoto(photo) {
  return $(`
    <div id="${photo.epoch}" class="col-lg-4 col-md-6 col-sm-12">
      <figure class="figure">
        <div class="figure-img-container">
          <img
            src="${photo.src}"
            class="img-fluid rounded"
            alt="${photo.location}"
          />
        </div>
        <div class="row figure-caption-container">
          <figcaption class="figure-caption photo-location">
            ${photo.location}
          </figcaption>
          <span class="dot"></span>
          <figcaption class="figure-caption photo-date">
            ${photo.date}
          </figcaption>
        </div>
      </figure>
  </div>`);
}

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

/**
 * Generate html components for each photo in photosData
 */
function generatePhotoComponents() {
  for (const photo of photosData) {
    // add epoch time to each photo
    const date = photo.date.split(' ');
    const year = date[2];
    const month = monthToNum[date[0]];
    const day = date[1].slice(0, -2);
    const epochTime = new Date(year, month, day).getTime() / 1000;
    photo.epoch = epochTime;
    // create photo component
    const component = createPhoto(photo);
    photo.component = component;
    allComponents.push(component);
  }
}

/**
 * Map photos into gallery
 * @param {string} components the list of specific photo components to display
 */
function mapPhotos(components = allComponents) {
  $('#gallery').empty();
  $('#gallery').append(components);
  $('#newest').button('toggle');
  $('#oldest').button('dispose');
}

/**
 * Order photos based on date
 * @param order the prefered ording to display photos (newest first vs oldest first)
 */
function sortPhotos(order = 'newest') {
  const components = $('#gallery').contents().toArray();
  if (order == 'oldest') {
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
  const components = [];
  for (const photo of photosData) {
    if (filter == null || photo.tags.includes(filter)) {
      components.push(photo.component);
    }
  }
  mapPhotos(components);
}

// eventListeners for filtering photos based on an attribute
$('#filter-beach').click(() => filterPhotos('beach'));
$('#filter-sunset').click(() => filterPhotos('sunset'));
$('#filter-mountain').click(() => filterPhotos('mountain'));
$('#filter-none').click(() => filterPhotos());

// eventListeners for sorting photos
$('#newest').click(() => sortPhotos('newest'));
$('#oldest').click(() => sortPhotos('oldest'));

/**
 * fetch text from /data to display
 */
function getFetchRequest() {
  const comments = [];
  const commentIds = [];
  fetch('/data')
    .then((response) => response.json())
    .then((json) => {
      for (const comment of json) {
        const linkClass = `"rec-link${comment.link ? '' : ' empty-link'}"`;
        const component = $(`
          <div class="comment">
              <button class="btn btn-block text-left rec-location" id="btn${comment.id}" type="button" data-toggle="collapse" data-target="#rec-${
                comment.id
              }" aria-expanded="false" aria-controls="rec-${comment.id}">
                <i class="fa fa-caret-right"></i>
                ${comment.location}
              </button>
              <div id="rec-${
                comment.id
              }" class="collapse" aria-labelledby="rec-${comment.id}">
                <div class="rec-description">
                  <div class="rec-description-txt">${
                    comment.description || 'No Description.'
                  }</div>
                  <a href=${
                    comment.link
                  } target="_blank" rel="noopener noreferrer" class=${linkClass}>Learn more</a>
                </div>
              </div>
            </div>
        `);
        comments.push(component);
        commentIds.push(`btn${comment.id}`);
        console.log('in fetch for loop')
        console.log(`btn${comment.id}`)
      }
      $('#comments').empty();
      $('#comments').append(comments);
    });
    
  console.log('post fetch for loop')
  console.log(commentIds)
  for (const cid of commentIds) {
    console.log('in for loop')
    console.log(cid)
    $(`#${cid}`).on('click', () => {
      console.log('clicked')
      console.log(this)
      $(this).find('i').animate(
        {rotation: 90}
      )
    })
  }
}

$(document).ready(() => {
  generatePhotoComponents();
  mapPhotos();
  sortPhotos();
  getFetchRequest();
});
