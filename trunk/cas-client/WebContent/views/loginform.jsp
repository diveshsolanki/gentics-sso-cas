<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<form method="post" action="<%= request.getAttribute("credentialValidateUrl") %>">
	<input type="hidden" name="lt" value="<%= request.getAttribute("loginToken") %>" />
	<input type="hidden" name="on_error" value="<%= request.getAttribute("onErrorUrl") %>" />
</form>
