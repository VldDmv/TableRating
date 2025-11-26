<%-- Admin dashboard displaying statistics for users and categories, served by AdminServlet --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Admin Dashboard</title>
    <!-- Links to CSS for user menu and dashboard styling -->
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/main.css">
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/pages/admin.css">
</head>
<body class="admin-page">

<%@ include file="/WEB-INF/fragments/header.jspf" %>

<!-- Admin navigation menu  -->
<div class="admin-nav">
    <a href="${pageContext.request.contextPath}/admin/dashboard">Dashboard</a>
    <a href="${pageContext.request.contextPath}/admin/users">Manage Users</a>
    <a href="${pageContext.request.contextPath}/admin/management?type=tags">Manage Tags</a>
    <a href="${pageContext.request.contextPath}/admin/management?type=genres">Manage Genres</a>
</div>

<!-- Main content container -->
<div class="container">
    <h1>Admin Dashboard</h1>

    <!-- Grid of statistics cards -->
    <div class="stats-grid">
        <div class="stat-card">
            <h3>Total Users</h3>
            <p class="stat-number"><c:out value="${stats.totalUsers}"/></p>
        </div>
        <div class="stat-card">
            <h3>Total Games</h3>
            <p class="stat-number"><c:out value="${stats.totalGames}"/></p>
        </div>
        <div class="stat-card">
            <h3>Total Movies</h3>
            <p class="stat-number"><c:out value="${stats.totalMovies}"/></p>
        </div>
        <div class="stat-card">
            <h3>Total Books</h3>
            <p class="stat-number"><c:out value="${stats.totalBooks}"/></p>
        </div>
        <div class="stat-card">
            <h3>Total Shows</h3>
            <p class="stat-number"><c:out value="${stats.totalShows}"/></p>
        </div>
    </div>
</div>

</body>
</html>