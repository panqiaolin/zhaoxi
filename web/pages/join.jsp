<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta charset="utf-8">
<title>朝夕</title>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<link href="../static/thirdparty/flat-ui/bootstrap/css/bootstrap.css"
	rel="stylesheet">
<link href="../static/thirdparty/flat-ui/css/flat-ui.css"
	rel="stylesheet">
<link rel="shortcut icon"
	href="../static/thirdparty/flat-ui/images/favicon.ico">
<!--[if lt IE 9]>
      <script src="../static/thirdparty/flat-ui/js/html5shiv.js"></script>
      <script src="../static/thirdparty/flat-ui/js/respond.min.js"></script>
    <![endif]-->
<link rel="stylesheet" href="../static/css/register.css">
</head>
<body>
	<div class="container">
		<div class="form-wrapper">
			<form id="register" action="join.do" method="post" enctype="multipart/form-data">
			<input type="hidden" name="action" value="reg" />
				<div class="form-group">
					<input type="text"  name="username" class="form-control" placeholder="用户名" />
				</div>
				<div class="form-group">
					<input type="password" class="form-control"  name="password" placeholder="密码" />
				</div>
				<div class="form-group">
					<input type="text" class="form-control" name="email" placeholder="email" />
				</div>
				<div class="form-group">
					<input type="file" class="form-control"  name="file"/> <span class="tip">请上传证件照片</span>
				</div>
				<div class="form-group">
					<input type="submit" class="btn btn-embossed btn-primary btn-wide"
						value="注册" />
				</div>
			</form>
			<form id="login">
				<div class="form-group">
					<input type="text" class="form-control" placeholder="用户名" />
				</div>
				<div class="form-group">
					<input type="text" class="form-control" placeholder="密码" />
				</div>
				<div class="form-group">
					<input type="submit" class="btn btn-embossed btn-primary btn-wide"
						value="登录" />
				</div>
			</form>
		</div>
		<button class="btn btn-embossed btn-default btn-wide toggle-btn"
			id="toggle">登录</button>
	</div>

	<script src="../static/thirdparty/flat-ui/js/jquery-1.8.3.min.js"></script>
	<script
		src="../static/thirdparty/flat-ui/js/jquery-ui-1.10.3.custom.min.js"></script>
	<script
		src="../static/thirdparty/flat-ui/js/jquery.ui.touch-punch.min.js"></script>
	<script src="../static/thirdparty/flat-ui/js/bootstrap.min.js"></script>
	<script src="../static/thirdparty/flat-ui/js/bootstrap-select.js"></script>
	<script src="../static/thirdparty/flat-ui/js/bootstrap-switch.js"></script>
	<script src="../static/thirdparty/flat-ui/js/flatui-checkbox.js"></script>
	<script src="../static/thirdparty/flat-ui/js/flatui-radio.js"></script>
	<script src="../static/thirdparty/flat-ui/js/jquery.tagsinput.js"></script>
	<script src="../static/thirdparty/flat-ui/js/jquery.placeholder.js"></script>
	<script>
		$(function() {
			var $toggle = $('#toggle');
			var $forms = $('#register, #login');
			var lock = true;
			$toggle.on('click', function(e) {
				$forms.toggle();
				this.innerHTML = lock ? '注册' : '登录';
				$forms.filter(':visible').find('input:first').focus();
				login = !lock;
			});
		});
	</script>
</body>
</html>