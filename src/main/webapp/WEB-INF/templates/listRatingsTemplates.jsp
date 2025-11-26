<%-- Template for rendering category item lists with forms, pagination, and modals --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page session="true" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${entityNamePlural}"/> List</title>
    <meta name="_csrf_token" content="${sessionScope._csrfToken}">
    <meta name="_entity_type" content="${entityType}">
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/main.css">
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/pages/category.css">
</head>
<body class="${entityType}-page">

<%@ include file="/WEB-INF/fragments/header.jspf" %>

<main class="page-container">
    <%-- Flash messages --%>
    <%@ include file="/WEB-INF/fragments/flashMessages.jspf" %>

    <%-- Navigation --%>
    <c:set var="backLink" value="/jsp/dashboard.jsp"/>
    <c:set var="backLinkText" value="Back to Category Selection"/>
    <%@ include file="/WEB-INF/fragments/navigation.jspf" %>

    <h2>Add a <c:out value="${entityNameSingular}"/></h2>
    <form action="${pageContext.request.contextPath}/${entityType}" id="${addFormId}" method="post">


        <div class="form-input-row">
            <input type="text" name="${paramNames.addItemName}" placeholder="<c:out value='${entityNameSingular}'/> Name" required>
            <input type="number" name="${paramNames.addItemScore}" placeholder="Score (1-100)" required min="1" max="100">
        </div>

        <%-- Tag/Genre selector --%>
        <c:set var="selectorTitle" value="${entityType == 'games' ? 'Tags' : 'Genres'}"/>
        <c:set var="selectorItems" value="${entityType == 'games' ? allTags : allGenres}"/>
        <c:set var="paramName" value="${paramNames.addItemTagIds}"/>
        <%@ include file="/WEB-INF/fragments/tagGenreSelector.jspf" %>

        <input type="hidden" name="_csrf" value="${sessionScope._csrfToken}">
        <button type="submit">Add <c:out value="${entityNameSingular}"/></button>
    </form>

    <h2>Your <c:out value="${entityNamePlural}"/></h2>

    <div class="column-controls">
        <div id="column-toggle-container"></div>
    </div>

    <div class="table-controls">
        <label>Show: <select id="rowsPerPage">
            <option value="10">10</option>
            <option value="25">25</option>
            <option value="50">50</option>
            <option value="100">100</option>
            <option value="200">200</option>
            <option value="500">500</option>
        </select></label>
        <label>Search: <input type="text" id="searchBox" placeholder="Search <c:out value="${entityNamePlural}"/>..."></label>
        <label>Filter by ${entityType == 'games' ? 'Tag' : 'Genre'}:
            <select id="tagFilter">
                <option value="all">All</option>
                <c:forEach var="item" items="${entityType == 'games' ? allTags : allGenres}">
                    <option value="${item.id}">${item.name}</option>
                </c:forEach>
            </select>
        </label>

    </div>

    <table data-entity-type="${entityType}">
        <thead>
        <tr>
            <th class="col-name"><c:out value="${entityNameSingular}"/></th>
            <th class="col-score">Score</th>
            <th class="col-tags">${entityType == 'games' ? 'Tags' : 'Genres'}</th>
            <th class="col-completed">Completed</th>
            <th class="col-actions">Actions</th>
        </tr>
        </thead>
        <tbody id="${entityType}Body"></tbody>
    </table>

    <div class="pagination-controls-container">
        <div class="pagination-controls">
            <button id="prevPage">&laquo;</button>
            <div class="relative-container">
                <button id="pageDropdown">Page 1 of 4</button>
                <ul id="pageList">
                    <%-- JavaScript --%>
                </ul>
            </div>
            <button id="nextPage">&raquo;</button>
        </div>
    </div>

    <div id="tags-edit-modal" class="modal">
        <div class="modal-content">
            <span class="close">&times;</span>
            <h2>Edit Tags</h2>
            <div class="modal-body"></div>
            <div class="modal-footer">
                <button id="modal-save-tags" class="button-primary">Save Tags</button>
                <button id="modal-cancel-tags">Cancel</button>
            </div>
        </div>
    </div>
</main>

<script>
    // Initialize available items for JavaScript
    <c:set var="itemsList" value="${entityType == 'games' ? allTags : allGenres}"/>
    <c:set var="itemsVarName" value="${entityType == 'games' ? 'allAvailableTags' : 'allAvailableGenres'}"/>

    window.${itemsVarName} = [
        <c:forEach var="item" items="${itemsList}" varStatus="loop">
        { id: ${item.id}, name: "<c:out value='${item.name}' escapeXml='true'/>" }${!loop.last ? ',' : ''}
        </c:forEach>
    ];
</script>
<script>
    const allTagsJsonString = '<c:out value="${allTagsJson}" escapeXml="true"/>';
</script>
<script id="initial-page-data" type="application/json">
    ${gson.toJson(pageResult)}
</script>
<script type="module" src="${pageContext.request.contextPath}/js/tableScripts/main.js"></script>
</body>
</html>