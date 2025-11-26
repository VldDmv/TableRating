<%-- Displays a user's profile with category tabs, handled by ProfileServlet and ProfileDataServlet --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page session="true" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${profileOwner.name}"/>'s Profile</title>
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/main.css">
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/pages/profile.css">
</head>
<body>
    <%@ include file="/WEB-INF/fragments/header.jspf" %>

    <div class="profile-container">
        <div class="profile-header">
            <h1><c:out value="${profileOwner.name}"/>'s Profile</h1>

            <%-- Navigation --%>
            <c:set var="backLink" value="/users"/>
            <c:set var="backLinkText" value="Back to User List"/>
            <%@ include file="/WEB-INF/fragments/navigation.jspf" %>
        </div>

        <c:if test="${isOwnerViewing}">
            <div class="privacy-settings">
                <h3>Profile Settings</h3>
                <form action="${pageContext.request.contextPath}/profile" method="post">
                    <input type="hidden" name="_csrf" value="${sessionScope._csrfToken}">
                    <fieldset>
                        <legend>Profile Visibility:</legend>
                        <div>
                            <input type="radio" id="privacy-public" name="privacy" value="public"
                                   <c:if test="${profileOwner.profileIsPublic}">checked</c:if>>
                            <label for="privacy-public">Public (Everyone can see your lists)</label>
                        </div>
                        <div>
                            <input type="radio" id="privacy-private" name="privacy" value="private"
                                   <c:if test="${!profileOwner.profileIsPublic}">checked</c:if>>
                            <label for="privacy-private">Private (Only you can see your lists)</label>
                        </div>
                    </fieldset>
                    <button type="submit" class="button-save-privacy">Save Settings</button>
                </form>
            </div>
        </c:if>

        <c:choose>
            <c:when test="${canView}">
                <div class="profile-tabs">
                    <button class="tab-link active" data-category="games">Games</button>
                    <button class="tab-link" data-category="movies">Movies</button>
                    <button class="tab-link" data-category="books">Books</button>
                    <button class="tab-link" data-category="shows">Shows</button>
                </div>

                <div id="tab-content-container" class="tab-content"></div>
            </c:when>
            <c:otherwise>
                <div class="private-profile-message">
                    <h2>This Profile is Private</h2>
                    <p>The user <c:out value="${profileOwner.name}"/> has chosen to keep their lists private.</p>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

   <c:if test="${canView}">
       <script>
           window.profileConfig = {
               username: "<c:out value='${profileOwner.name}'/>",
               initialResult: <c:out value="${gson.toJson(pageResult)}" escapeXml="false"/>,
               contextPath: "${pageContext.request.contextPath}"
           };
       </script>
       <script type="module" src="${pageContext.request.contextPath}/js/profileScripts.js"></script>
   </c:if>
</body>
</html>