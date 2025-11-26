 <%-- Admin page for managing tags or genres, handled by AdminServlet --%>
 <%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
 <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
 <%@ page session="true" %>
 <!DOCTYPE html>
 <html lang="en">
 <head>
     <meta charset="UTF-8">
     <title>Manage ${itemType}s</title>
     <!-- Links to CSS for table, user menu, and modal styling -->
     <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/main.css">
     <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/pages/admin.css">
 </head>
 <body class="admin-page">

 <%@ include file="/WEB-INF/fragments/header.jspf" %>

 <!-- Admin navigation menu -->
 <div class="admin-nav">
     <a href="${pageContext.request.contextPath}/admin/dashboard">Dashboard</a>
     <a href="${pageContext.request.contextPath}/admin/users">Manage Users</a>
     <a href="${pageContext.request.contextPath}/admin/management?type=tags">Manage Tags</a>
     <a href="${pageContext.request.contextPath}/admin/management?type=genres">Manage Genres</a>
 </div>

 <div class="container">
     <h1>Manage ${itemType}s</h1>

     <!-- Flash messages for success or error -->
     <c:if test="${not empty sessionScope.flashErrorMessage}">
         <div class="flash-error-message">${sessionScope.flashErrorMessage}</div>
         <c:remove var="flashErrorMessage" scope="session"/>
     </c:if>
     <c:if test="${not empty sessionScope.flashSuccessMessage}">
         <div class="flash-success-message">${sessionScope.flashSuccessMessage}</div>
         <c:remove var="flashSuccessMessage" scope="session"/>
     </c:if>

     <!-- Form to add new tag or genre -->
     <div class="add-form-container">
         <h2>Add new ${itemType}</h2>
         <form action="${pageContext.request.contextPath}/admin/management" method="post">
             <input type="hidden" name="type" value="${type}">
             <input type="hidden" name="action" value="add">
             <input type="hidden" name="_csrf" value="${sessionScope._csrfToken}">
             <label for="name">Name:</label>
             <input type="text" id="name" name="name" required>
             <c:if test="${type == 'genres'}">
                 <!-- Media type selection for genres -->
                 <fieldset class="media-type-selection">
                     <legend>Applicable To:</legend>
                     <label><input type="checkbox" name="mediaTypes" value="movie"> Movies</label>
                     <label><input type="checkbox" name="mediaTypes" value="book"> Books</label>
                     <label><input type="checkbox" name="mediaTypes" value="show"> Shows</label>
                     <label><input type="checkbox" name="mediaTypes" value="shared" checked> Shared (All)</label>
                 </fieldset>
             </c:if>
             <button type="submit">Add ${itemType}</button>
         </form>
     </div>

     <h2>Existing ${itemType}s</h2>

     <!-- Table displaying existing tags or genres -->
     <table>
         <thead>
             <tr>
                 <th>ID</th>
                 <th>Name</th>
                 <c:if test="${type == 'genres'}"><th>Applicable To</th></c:if>
                 <th>Actions</th>
             </tr>
         </thead>
         <tbody>
             <c:forEach var="item" items="${items}">
                 <tr>
                     <td>${item.id}</td>
                     <td><c:out value="${item.name}"/></td>
                     <c:if test="${type == 'genres'}">
                         <td>
                             <c:forEach var="mediaType" items="${item.mediaTypes}" varStatus="loop">
                                 <span class="tag-badge"><c:out value="${mediaType}"/></span>${!loop.last ? ' ' : ''}
                             </c:forEach>
                         </td>
                     </c:if>
                     <td>
                         <!-- Button to open edit modal -->
                         <button class="edit-btn"
                                 data-id="${item.id}"
                                 data-name="<c:out value="${item.name}"/>"
                                 <c:if test="${type == 'genres'}">
                                     data-media-types='${String.join(",", item.mediaTypes)}'
                                 </c:if>>
                             Edit
                         </button>
                         <!-- Form to delete tag or genre -->
                         <form action="${pageContext.request.contextPath}/admin/management" method="post" class="admin-inline-form">
                             <input type="hidden" name="type" value="${type}">
                             <input type="hidden" name="action" value="delete">
                             <input type="hidden" name="id" value="${item.id}">
                             <input type="hidden" name="_csrf" value="${sessionScope._csrfToken}">
                             <button type="submit" class="delete-button" onclick="return confirm('Are you sure you want to delete this item? It cannot be undone.')">Delete</button>
                         </form>
                     </td>
                 </tr>
             </c:forEach>
         </tbody>
     </table>
 </div>

 <!-- Modal for editing tags or genres -->
 <div id="editModal" class="modal">
     <div class="modal-content">
         <span class="close-btn">&times;</span>
         <h2>Edit ${itemType}</h2>
         <form id="edit-form" action="${pageContext.request.contextPath}/admin/management" method="post">
             <input type="hidden" name="type" value="${type}">
             <input type="hidden" name="action" value="update">
             <input type="hidden" name="id" id="edit-id">
             <input type="hidden" name="_csrf" value="${sessionScope._csrfToken}">
             <label for="edit-name">Name:</label>
             <input type="text" id="edit-name" name="name" required>

             <c:if test="${type == 'genres'}">
                 <!-- Media type selection for editing genres -->
                 <fieldset id="edit-media-types" class="media-type-selection">
                     <legend>Applicable To:</legend>
                     <label><input type="checkbox" name="mediaTypes" value="movie"> Movies</label>
                     <label><input type="checkbox" name="mediaTypes" value="book"> Books</label>
                     <label><input type="checkbox" name="mediaTypes" value="show"> Shows</label>
                     <label><input type="checkbox" name="mediaTypes" value="shared"> Shared (All)</label>
                 </fieldset>
             </c:if>
             <button type="submit">Save Changes</button>
         </form>
     </div>
 </div>

 <!-- Script for modal interaction -->
<script type="module" src="${pageContext.request.contextPath}/js/managementScripts.js"></script>

 </body>
 </html>