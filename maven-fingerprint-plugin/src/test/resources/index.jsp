<!DOCTYPE html>
<html lang="en" style="position:relative;min-height: 98%;">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta http-equiv="Cache-Control" content="max-age=0" />
	<meta http-equiv="Cache-Control" content="no-cache" />
	<meta http-equiv="Expires" content="0" />
	<meta http-equiv="Expires" content="Tue, 01 Jan 1980 1:00:00 GMT" />
	<meta http-equiv="Pragma" content="no-cache" />
    <title>Empath - Social Monitoring Tools</title>
    <link href="${pageContext.request.contextPath}/css/themes/wds/css/custom-bootstrap.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/app.css" rel="stylesheet" >

    <!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
      <script src="https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js"></script>
      <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->

  	<script data-main="${pageContext.request.contextPath}/js/app/main.js" type="text/javascript" src="${pageContext.request.contextPath}/js/lib/require.js"></script>
	<script type="text/javascript">
    	document.contextPath = '<%= request.getContextPath() %>';
    	document.sessionTimeout = '${sessionTimeout}';
    </script>
  </head>
  <body style="margin:0 0 50px;">
  </body>
</html>