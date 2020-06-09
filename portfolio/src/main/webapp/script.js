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
 * Supported ways to load comments.
 * LOAD: load the default number of comments
 * RELOAD: reload the existing number of comments
 * APPEND: load an additional batch of comments which is then appended to current comments
 * @enum {string}
 */
const LoadType = {
  LOAD: "load",
  RELOAD: "reload",
  APPEND: "append",
};

/**
 * return the datastore id of a comment
 * @param {string} cid the id attribute of a comment html component
 * @return {number}
 */
function getCommentId(cid) {
  const idElems = cid.split("-");
  return Number(idElems[idElems.length - 1]);
}

/**
 * Create photo component with caption
 * @param {Photo} photo a Photo object that contains the data for a photo html component
 * @return {JQuery}
 */
function createPhoto(photo) {
  return $(`
    <div id="${photo.epoch}" class="col-lg-4 col-md-6 col-xs-12">
      <figure class="figure">
        <div class="figure-img-container">
          <img
            src="${photo.src}"
            class="img-fluid rounded"
            alt="${photo.location}"
          />
          <div class="overlay">
            <button 
              type="button" 
              id="btn-${photo.epoch}" 
              class="btn btn-outline-secondary view-map-btn"
              data-toggle="modal" 
              data-target="#map-modal" 
              data-location="${photo.location}"
              data-lat="${photo.lat}"
              data-lng="${photo.lng}"
            >
              View Map
            </button>
          </div>
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
 * Generate html components for each photo in photosData.
 */
function generatePhotoComponents() {
  for (const photo of photosData) {
    // add epoch time to each photo
    const date = photo.date.split(" ");
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
  $("#gallery").empty();
  $("#gallery").append(components);
  $("#newest").button("toggle");
  $("#oldest").button("dispose");
}

/**
 * Order photos based on date
 * @param order the prefered ording to display photos (newest first vs oldest first)
 */
function sortPhotos(order = "newest") {
  const components = $("#gallery").contents().toArray();
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
  const components = [];
  for (const photo of photosData) {
    if (filter == null || photo.tags.includes(filter)) {
      components.push(photo.component);
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
$("#newest").click(() => sortPhotos("newest"));
$("#oldest").click(() => sortPhotos("oldest"));

/**
 * Create comment component with location, description, and link
 * @param {json} comment a json object that contains the data for a comment html component
 * @return {JQuery}
 */
function createComment(comment) {
  const linkClass = `"rec-link col-10${comment.link ? "" : " empty-link"}"`;
  const deleteButton =
    $("#user-email").text() === comment.userEmail
      ? `
        <i 
          class="fa fa-ban col-2" 
          id="delete-btn-${comment.id}" 
          data-toggle="tooltip" 
          data-placement="top" 
          title="delete recommendation"
        ></i>
        `
      : "";

  return $(`
    <div class="comment">
      <button
        class="btn btn-block text-left rec-location container"
        id="btn-${comment.id}"
        type="button"
        data-toggle="collapse"
        data-target="#rec-${comment.id}"
        aria-expanded="false"
        aria-controls="rec-${comment.id}"
      >
        <div class="row">
          <div class="col-md-6 col-sm-12">
            <i class="fa fa-caret-right"></i>
            ${comment.location}
          </div>
          <div class="col-md-6 col-sm-12 comment-name">
            ${comment.displayedName}
          </div>
        </div>
      </button>
      <div
        id="rec-${comment.id}"
        class="collapse"
        aria-labelledby="rec-${comment.id}"
      >
        <div class="rec-description">
          <div class="rec-description-txt">${
            comment.description || "No Description."
          }</div>
          <div class="row">
            <a
              href=${comment.link}
              target="_blank"
              rel="noopener noreferrer"
              class=${linkClass}
              >Learn more</a
            >
            ${deleteButton}
          </div>
        </div>
      </div>
    </div>
  `);
}

/**
 * Delete the comment selected from datastore, reload existing comments
 * @param {string} cid comment id used to distinguish each unique comment
 */
function deleteComment(cid) {
  const id = getCommentId(cid);
  $.ajax({
    type: "POST",
    url: `/delete-data?id=${id}`,
    success: () =>
      $(".comment").length > 1
        ? loadComments(LoadType.RELOAD)
        : loadComments(LoadType.LOAD),
  }).done(() => {
    if ($(".comment").length === 1) {
      $("#rec-count").hide();
    }
  });
}

/**
 * fetch comments from datastore to display
 * @param {string} type the request parameter
 */
function loadComments(type = LoadType.LOAD) {
  $('#load-more-btn-txt').text('Loading . . .');

  const numComments = $(".comment").length;
  const comments = [];
  const commentIds = [];
  fetch(`/data?type=${type}&numComments=${numComments}`)
    .then((response) => response.json())
    .then((json) => {
      // indicate if there are no comments
      if (jQuery.isEmptyObject(json.comments)) {
        $("#comments")
          .children()
          .replaceWith('<div class="empty-notice">No Recommendations</div>');
        $("#load-more-btn").prop("disabled", true);
        return;
      }

      // add comments to DOM
      for (const comment of json.comments) {
        const component = createComment(comment);
        comments.push(component);
        commentIds.push(`btn-${comment.id}`);
      }
      if (type === LoadType.LOAD || type === LoadType.RELOAD) {
        $("#comments").empty();
      }
      $("#comments").append(comments);

      // comments count & load more button
      const numLoaded = $(".comment").length;
      $("#rec-count").show();
      $("#rec-count").text(`Comments: ${numLoaded}/${json.total}`);
      $("#load-more-btn").prop("disabled", numLoaded === json.total);
    })
    .then(() => {
      // add individual event listeners per comment
      for (const cid of commentIds) {
        $(`#${cid}`).click(() => {
          $(`#${cid}`).find(".fa-caret-right").toggleClass("rotated");
        });
        $(`#delete-${cid}`).click(() => deleteComment(cid));
      }

      $('#load-more-btn-txt').text('Load More');
    });
}

$('#load-more-btn').click(() => loadComments(LoadType.APPEND));

/**
 * Make POST request to /data upon submission of recommendation form to add comment to datastore.
 */
function handleSubmit(e) {
  e.preventDefault();
  $.ajax({
    type: "POST",
    url: "/data",
    data: $(this).serialize(),
    success: () =>
      $(".comment").length > 0
        ? loadComments(LoadType.RELOAD)
        : loadComments(LoadType.LOAD),
  });
  $(this).find("input,textarea").val("");
  $("#anonCheck").prop("checked", false);
  $("#displayedName").prop("required", true);
}

$("#rec-form").submit(handleSubmit);
$("#anonCheck-container").click(() => {
  $("#displayedName").prop("required", !$("#anonCheck").prop("checked"));
});

/**
 * Check if user is logged in to Google account.
 * If they are logged in, display comments form, else display button to log in.
 */
function checkLogin() {
  fetch("/auth")
    .then((res) => res.json())
    .then((json) => {
      if (json.isLoggedIn) {
        $("#rec-form").show();
        $("#login").hide();
        $("#user-email").text(json.userEmail);
        $("#logout-btn").click(() => window.open(json.url, "_self"));
      } else {
        $("#rec-form").hide();
        $("#login").show();
        $("#login-btn").click(() => window.open(json.url, "_self"));
      }
    });
}

// ref: https://getbootstrap.com/docs/4.0/components/modal/#varying-modal-content
$("#map-modal").on("show.bs.modal", function (event) {
  // button that triggered show modal event
  const button = $(event.relatedTarget);

  // get data passed by the button
  const location = button.data("location");
  const lat = button.data("lat");
  const lng = button.data("lng");

  // set content of modal based on the button pressed
  const modal = $(this);
  modal.find(".modal-title").text(location);
  initMap(lat, lng);
});

/**
 * use Google maps API to create a map with a marker at the given coordinates
 * @param {number} latitude
 * @param {number} longitude
 */
function initMap(latitude, longitude) {
  const location = { lat: latitude, lng: longitude };
  const map = new google.maps.Map(document.getElementById("map"), {
    zoom: 16,
    center: location,
  });
  const marker = new google.maps.Marker({ position: location, map });
}

$(document).ready(() => {
  generatePhotoComponents();
  mapPhotos();
  sortPhotos();
  checkLogin();
  loadComments(LoadType.LOAD);
});
