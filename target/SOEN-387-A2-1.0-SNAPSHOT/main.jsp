<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
    <link rel="stylesheet" type="text/css" href="style.css" media="screen"/>
</head>
<body>
<div class="topnav">
    <a class="active" href="#home">Home</a>
    <a href="#news">News</a>
    <a href="#contact">Contact</a>
    <a href="#about">About</a>
    <a href="login.jsp">Login</a>
    <input type="text" placeholder="Search..">
</div>


<hr>
<section>
    <h1>Chat area</h1>
    <form action="MessageBoardServlet" method="POST">
        <input type="text" name="message">
        <input type="file" name="attachment" value="Add attachment"> <br><br>
        <input type="submit" name="create-post" value="Create">
        <button type="button">Delete Post!</button>
        <button type="button">Update Post!</button>
        <button type="button">View recent Post!</button>
        <button type="button">Download!</button>
        <button type="button">Clear Chat!</button>

        <button type="button">???!</button>
    </form>
    <br>
    <hr>
    <form action="UploadServlet" method="post" enctype="multipart/form-data">
        <input type="file" required="" name="photo">
        <input type="submit" value="Save">
    </form>
    <%
        if (request.getAttribute("uploaded_message") != null) {
    %>
    <h3><%=request.getAttribute("uploaded_message")%></h3>
    <%
        }
    %>
</section>
</body>
</html>