package com.alidao.basic.web.control;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alidao.basic.entity.Manager;
import com.alidao.basic.service.ManagerService;
import com.alidao.common.Constants;
import com.alidao.jse.util.Crypto;
import com.alidao.jse.util.StringUtil;
import com.alidao.jxe.control.WebCtrl;
import com.alidao.jxe.help.PowerHelper;
import com.alidao.jxe.model.Condition;
import com.alidao.jxe.model.PageParam;
import com.alidao.jxe.model.ParamException;
import com.alidao.jxe.model.ResponseForAjax;

@Controller
@RequestMapping("manager")
public class ManagerCtrl extends WebCtrl {

	@Autowired
	private ManagerService managerService;

	@RequestMapping("init")
	public void init() {}

	@RequestMapping("list")
	public void list(PageParam pageParam, Manager object,
			HttpServletResponse response) throws IOException {
		object.addCondition("username != 'admin'", Condition.SEP_AND);
		managerService.page(pageParam, object).jsonOut(response);
	}

	@RequestMapping("edit")
	public void edit(Model model, String id) {
		if (StringUtil.isNotBlank(id)) {
			model.addAttribute("object", managerService.find(id));
		}
	}

	@RequestMapping("save")
	public void save(Manager object, HttpServletResponse response)
			throws IOException, ParamException {
		if (StringUtil.isNotBlank(object.getPassword())) {
			object.setPassword(Crypto.MD5(object.getPassword()));
		}
		if (StringUtil.isEmpty(object.getId())) {
			verifyEntity(object, Manager.VERIFY_FIELDS);
			getResponse(managerService.save(object)).jsonOut(response);
		} else {
			getResponse(managerService.mdfy(object)).jsonOut(response);
		}
	}

	@RequestMapping("reset/{id}")
	public void reset(@PathVariable("id") String id, 
			HttpServletResponse response) throws IOException {
		Manager object = new Manager();
		object.setId(id);
		object.setPassword(Crypto.MD5("123456"));
		getResponse(managerService.mdfy(object)).jsonOut(response);
	}

	@RequestMapping("active/{id}")
	public void active(@PathVariable("id") String id, 
			HttpServletResponse response) throws IOException {
		Manager object = new Manager();
		object.setId(id);
		object.setStatus(Manager.NORMAL);
		getResponse(managerService.mdfy(object)).jsonOut(response);
	}

	@RequestMapping("paused/{id}")
	public void paused(@PathVariable("id") String id, 
			HttpServletResponse response) throws IOException {
		Manager object = new Manager();
		object.setId(id);
		object.setStatus(Manager.PAUSED);
		getResponse(managerService.mdfy(object)).jsonOut(response);
	}

	@RequestMapping("lose/{id}")
	public void lose(@PathVariable("id") String id, 
			HttpServletResponse response) throws IOException {
		getResponse(managerService.lose(id)).jsonOut(response);
	}

	@RequestMapping("login")
	public void login(String username, String password,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		ResponseForAjax resForAjax = new ResponseForAjax();
		Manager object = new Manager();
		object.setUsername(username);
		object = managerService.find(object);
		if (object != null) {
			password = Crypto.MD5(password);
			if (password.equals(object.getPassword())) {
				if (object.getStatus() == Manager.NORMAL) {
					resForAjax.setResult(1);
					resForAjax.setMessage("登录成功");
					PowerHelper.set(object.getId());
					Boolean super_power = false;
					if (Constants.get("power.super").contains(username)) {
						super_power = true;
					}
					HttpSession session = request.getSession();
					session.setAttribute("super_power", super_power);
				} else {
					resForAjax.setResult(-13);
					resForAjax.setMessage("该账号已被禁用");
				}
			} else {
				resForAjax.setResult(-12);
				resForAjax.setMessage("用户名或密码错误");
			}
		} else {
			resForAjax.setResult(-11);
			resForAjax.setMessage("账号不存在");
		}
		resForAjax.jsonOut(response);
	}

	@RequestMapping("logout")
	public void logout(HttpServletRequest request, 
			HttpServletResponse response) throws IOException {
		PowerHelper.lose();
		request.getSession().removeAttribute("super_power");
		getResponse(1).jsonOut(response);
	}

	@RequestMapping("repswd")
	public void repswd(String oldPswd, String newPswd, 
			HttpServletResponse response) throws IOException, ParamException {
		ResponseForAjax resForAjax = null;
		verifyParams(oldPswd, newPswd);
		String managerId = PowerHelper.get();
		Manager object = new Manager();
		object.setId(managerId);
		object = managerService.find(object);
		oldPswd = Crypto.MD5(oldPswd);
		if (oldPswd.equals(object.getPassword())) {
			object.setPassword(Crypto.MD5(newPswd));
			resForAjax = getResponse(managerService.mdfy(object));
		} else {
			resForAjax = new ResponseForAjax();
			resForAjax.setResult(-21);
			resForAjax.setMessage("旧密码不正确");
		}
		resForAjax.jsonOut(response);
	}

	@RequestMapping("find")
	public void find(Long id, String key, Integer type, HttpServletResponse response)
			throws IOException {
		ResponseForAjax resForAjax = new ResponseForAjax();
		Manager object = new Manager();
		if (type == 1) {
			object.setUsername(key);
		}
		object = managerService.find(object);
		if (object == null) {
			resForAjax.setResult(0); // 不存在，可以添加
		} else {
			if (id == null) {
				resForAjax.setResult(1); // 不是编辑，但是登录名存在，不能添加
			} else {
				if (object.getId().equals(id)) {
					resForAjax.setResult(0); // 编辑，是同一个登录名，可以编辑
				} else {
					resForAjax.setResult(1); // 编辑，不是同一个登录名，不可以编辑
				}
			}
		}
		resForAjax.jsonOut(response);
	}

}
