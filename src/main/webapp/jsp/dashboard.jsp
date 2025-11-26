<%-- Displays the dashboard for selecting a category (games, movies, books, shows, users) --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page session="true" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard</title>
       <!-- Links to CSS for styling dashboard and user menu -->
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/main.css">
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/pages/dashboard.css">
</head>
<body>

<%@ include file="/WEB-INF/fragments/header.jspf" %>
    <h1>Choose your category</h1>
     <!-- Container for category selection links -->
    <div class="choice-container">
    <!-- Links to category pages via ProfileServlet -->
   <a href="${pageContext.request.contextPath}/games" class="choice-brick brick-games">Games</a>
       <a href="${pageContext.request.contextPath}/movies" class="choice-brick brick-movies">Movies</a>
       <a href="${pageContext.request.contextPath}/books" class="choice-brick brick-books">Books</a>
       <a href="${pageContext.request.contextPath}/shows" class="choice-brick brick-shows">Shows</a>
       <a href="${pageContext.request.contextPath}/users" class="choice-brick brick-users">All Users</a>

            </a>
    </div>
</body>
</html>