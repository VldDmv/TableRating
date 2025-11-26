<%-- Handles user login and registration forms, processed by AuthServlet --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page session="true" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login / Register</title>
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/main.css">
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/pages/auth.css">
</head>
</head>
<body>
<div class="form-container" id="form-container">
    <%-- Login Form --%>
    <form id="login-form" action="${pageContext.request.contextPath}/auth" method="post">
        <h2>Login</h2>
        <label for="login-name">Name:</label>
        <input type="text" name="name" id="login-name" required>

        <label for="login-password">Password:</label>
        <input type="password" name="password" id="login-password" required minlength="6">

        <input type="hidden" name="action" value="login">
        <button type="submit">Login</button>

        <%-- Flash messages --%>
        <%@ include file="/WEB-INF/fragments/flashMessages.jspf" %>

        <p>Don't have an account? <a id="show-register" href="#">Register here</a></p>
    </form>

    <%-- Registration Form --%>
    <form id="register-form" action="${pageContext.request.contextPath}/auth" method="post" class="hidden">
        <h2>Register</h2>
        <label for="register-name">Name:</label>
        <input type="text" name="name" id="register-name" required>

        <label for="register-password">Password:</label>
        <input type="password" name="password" id="register-password"
               required minlength="6"
               pattern=".{6,}"
               title="Password must be at least 6 characters long">

        <label for="register-confirm-password">Confirm Password:</label>
        <input type="password" name="confirmPassword" id="register-confirm-password" required>

        <input type="hidden" name="action" value="register">
        <button type="submit" id="register-button">Register</button>

        <%-- Flash messages --%>
        <%@ include file="/WEB-INF/fragments/flashMessages.jspf" %>

        <p class="error-message" id="error-message"></p>
        <p>Already have an account? <a id="show-login" href="#">Login here</a></p>
    </form>
</div>

<script type="module" src="${pageContext.request.contextPath}/js/RegScripts.js"></script>
</body>
</html>