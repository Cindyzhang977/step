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

package com.google.sps.servlets;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.QueryResultList;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import com.google.sps.data.Comment;
import java.io.IOException;
import java.lang.String;
import java.util.Arrays;
import java.util.ArrayList;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that returns some example content. */
@WebServlet("/data")
public class DataServlet extends HttpServlet {

  /** Class to wrap around comments array and total number of comments, used to produce json. */
  private class CommentsWrapper {
    private int total = 0; 
    private ArrayList<Comment> comments;
    
    CommentsWrapper(int total, ArrayList<Comment> comments) {
      this.total = total;
      this.comments = comments;
    }
  }
  
  static final int LOAD_SIZE = 5;
  private final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
  private Cursor cursor;

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    ArrayList<Comment> comments = new ArrayList<>();
    int numComments = Math.max(Integer.parseInt(request.getParameter("numComments")), LOAD_SIZE);
    String type = request.getParameter("type");
    FetchOptions fetchOptions;
    
    switch (type) {
      case "reload":
        this.cursor = null;
        fetchOptions = FetchOptions.Builder.withLimit(numComments);
        break;
      case "append":
        fetchOptions = FetchOptions.Builder.withLimit(LOAD_SIZE);
        break;
      default:
        this.cursor = null;
        fetchOptions = FetchOptions.Builder.withLimit(LOAD_SIZE);
    }
     
    if (this.cursor != null) {
      fetchOptions.startCursor(this.cursor);
    }

    Query query = new Query("Comment").addSort("timestamp", SortDirection.DESCENDING);
    PreparedQuery pq = datastore.prepare(query);
    QueryResultList<Entity> results = pq.asQueryResultList(fetchOptions);
    for (Entity e : results) {
      String location = (String) e.getProperty("location");
      String link = (String) e.getProperty("link");
      String description = (String) e.getProperty("description");
      long id = e.getKey().getId();
      String userEmail = (String) e.getProperty("userEmail");
      String displayedName = (String) e.getProperty("displayedName");
      Comment c = new Comment(location, link, description, String.valueOf(id), userEmail, displayedName);
      comments.add(c);
    }
    this.cursor = results.getCursor();

    int total = pq.countEntities();
    CommentsWrapper cm = new CommentsWrapper(total, comments);

    Gson gson = new Gson();
    String json = gson.toJson(cm);
    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String location = request.getParameter("location");
    String link = request.getParameter("link");
    String description = request.getParameter("description");
    String userEmail = UserServiceFactory.getUserService().getCurrentUser().getEmail();
    String displayedName = request.getParameter("anonCheck") == null ? request.getParameter("displayedName") : "anon";

    Entity commentEntity = new Entity("Comment");
    commentEntity.setProperty("location", location);
    commentEntity.setProperty("link", link);
    commentEntity.setProperty("description", description);
    commentEntity.setProperty("userEmail", userEmail);
    commentEntity.setProperty("displayedName", displayedName);
    long timestamp = System.currentTimeMillis();
    commentEntity.setProperty("timestamp", timestamp);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(commentEntity);
  }
}
