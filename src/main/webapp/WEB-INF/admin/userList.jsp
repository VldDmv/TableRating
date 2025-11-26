<%-- Admin page for managing users, including role changes and deletion, handled by AdminServlet --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page session="true" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>User Management</title>
    <meta name="_csrf_token" content="${sessionScope._csrfToken}">
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
    <h1>User Management</h1>

    <!-- Flash messages for success or error -->
    <c:if test="${not empty sessionScope.flashSuccessMessage}">
        <div class="flash-success-message"><c:out value="${sessionScope.flashSuccessMessage}"/></div>
        <c:remove var="flashSuccessMessage" scope="session"/>
    </c:if>
    <c:if test="${not empty sessionScope.flashErrorMessage}">
        <div class="flash-error-message"><c:out value="${sessionScope.flashErrorMessage}"/></div>
        <c:remove var="flashErrorMessage" scope="session"/>
    </c:if>

    <!-- Search form for filtering users -->
    <div class="search-container">
        <form action="${pageContext.request.contextPath}/admin/users" method="get">
            <label for="searchBox">Search by Name:</label>
            <input type="text" id="searchBox" name="search" value="<c:out value="${requestScope.searchTerm}"/>" placeholder="Enter name...">
            <input type="hidden" name="page" value="1">
            <button type="submit">Search</button>
            <c:if test="${not empty requestScope.searchTerm}">
                <a href="${pageContext.request.contextPath}/admin/users?page=1">Clear Search</a>
            </c:if>
        </form>
    </div>

    <c:set var="users" value="${requestScope.userList}" />
    <c:set var="currentUser" value="${sessionScope.user}" />
    <c:set var="currentPage" value="${requestScope.currentPage}" />
    <c:set var="totalPages" value="${requestScope.totalPages}" />
    <c:set var="searchTerm" value="${requestScope.searchTerm}" />

    <c:choose>
        <c:when test="${not empty users}">
            <!-- Table displaying users with role change and delete options -->
            <table class="admin-users-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Name</th>
                        <th>Current Role</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="user" items="${users}">
                        <tr class="${currentUser.id == user.id ? 'current-admin-row' : ''}">
                            <td><c:out value="${user.id}" /></td>
                            <td><c:out value="${user.name}" /></td>
                            <td><c:out value="${user.role}" /></td>
                            <td class="admin-actions-cell">
                                <c:if test="${currentUser.id != user.id}">
                                    <!-- Form to change user role -->
                                    <form action="${pageContext.request.contextPath}/admin/changeRole" method="post" class="admin-inline-form">
                                        <input type="hidden" name="userId" value="${user.id}">
                                        <select name="newRole">
                                            <option value="USER" ${user.role.name() == 'USER' ? 'selected' : ''}>USER</option>
                                            <option value="ADMIN" ${user.role.name() == 'ADMIN' ? 'selected' : ''}>ADMIN</option>
                                        </select>
                                        <input type="hidden" name="_csrf" value="${sessionScope._csrfToken}">
                                        <button type="submit">Save Role</button>
                                    </form>
                                    <!-- Form to delete user -->
                                    <form action="${pageContext.request.contextPath}/admin/deleteUser" method="post"
                                          class="admin-inline-form"
                                          onsubmit="return confirm('Are you sure you want to delete user \'${fn:escapeXml(user.name)}\'? This action cannot be undone.');">
                                        <input type="hidden" name="userId" value="${user.id}">
                                        <input type="hidden" name="_csrf" value="${sessionScope._csrfToken}">
                                        <button type="submit" class="delete-user-button">Delete User</button>
                                    </form>
                                </c:if>
                                <c:if test="${currentUser.id == user.id}">
                                    <span class="current-admin-badge">(Current Admin)</span>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>

            <!-- Pagination controls -->
            <div class="pagination">
                <c:choose>
                    <c:when test="${currentPage > 1}">
                        <c:url var="prevUrl" value="/admin/users">
                            <c:param name="page" value="${currentPage - 1}" />
                            <c:if test="${not empty searchTerm}">
                                <c:param name="search" value="${searchTerm}" />
                            </c:if>
                        </c:url>
                        <a href="${prevUrl}">« Previous</a>
                    </c:when>
                    <c:otherwise>
                        <span class="disabled">« Previous</span>
                    </c:otherwise>
                </c:choose>

                <span>Page ${currentPage} of ${totalPages}</span>

                <c:choose>
                    <c:when test="${currentPage < totalPages}">
                        <c:url var="nextUrl" value="/admin/users">
                            <c:param name="page" value="${currentPage + 1}" />
                            <c:if test="${not empty searchTerm}">
                                <c:param name="search" value="${searchTerm}" />
                            </c:if>
                        </c:url>
                        <a href="${nextUrl}">Next »</a>
                    </c:when>
                    <c:otherwise>
                        <span class="disabled">Next »</span>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:when>
        <c:otherwise>
            <c:choose>
                <c:when test="${not empty searchTerm}">
                    <p>No users found matching your search criteria: "<c:out value="${searchTerm}"/>".</p>
                    <p><a href="${pageContext.request.contextPath}/admin/users?page=1">Clear Search</a></p>
                </c:when>
                <c:otherwise>
                    <p>No users found in the system.</p>
                </c:otherwise>
            </c:choose>
        </c:otherwise>
    </c:choose>

    <!-- Link to return to dashboard -->
    <p class="back-link"><a href="${pageContext.request.contextPath}/jsp/dashboard.jsp">« Back to Dashboard</a></p>
</div>

</body>
</html>