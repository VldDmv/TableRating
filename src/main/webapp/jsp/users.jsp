<%-- Displays a list of users with search and pagination, handled by ProfileServlet --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Users</title>
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/main.css">
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/pages/users.css">
</head>
<body>
    <%@ include file="/WEB-INF/fragments/header.jspf" %>

    <div class="container">
        <h1>All Users</h1>

        <%-- Navigation --%>
        <c:set var="backLink" value="/jsp/dashboard.jsp"/>
        <c:set var="backLinkText" value="Back to Dashboard"/>
        <%@ include file="/WEB-INF/fragments/navigation.jspf" %>

        <div class="search-container">
            <form action="${pageContext.request.contextPath}/users" method="get">
                <input type="text" name="search" placeholder="Search users..."
                       value="<c:out value='${searchTerm}'/>">
                <button type="submit">Search</button>
            </form>
        </div>

        <c:choose>
            <c:when test="${not empty userList}">
                <table class="user-list-table">
                    <thead>
                        <tr>
                            <th>Username</th>
                            <th>Profile</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="user" items="${userList}">
                            <tr>
                                <td><c:out value="${user.name}"/></td>
                                <td>
                                    <a href="${pageContext.request.contextPath}/profile?username=${user.name}"
                                       class="view-profile-link">View Profile</a>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:when>
            <c:otherwise>
                <p>No users found.</p>
            </c:otherwise>
        </c:choose>

        <%-- Simple inline pagination --%>
        <div class="pagination">
            <c:if test="${currentPage > 1}">
                <a href="?page=${currentPage - 1}<c:if test='${not empty searchTerm}'>&search=${searchTerm}</c:if>">&laquo; Previous</a>
            </c:if>

            <span>Page ${currentPage} of ${totalPages}</span>

            <c:if test="${currentPage < totalPages}">
                <a href="?page=${currentPage + 1}<c:if test='${not empty searchTerm}'>&search=${searchTerm}</c:if>">Next &raquo;</a>
            </c:if>
        </div>
    </div>

</body>
</html>