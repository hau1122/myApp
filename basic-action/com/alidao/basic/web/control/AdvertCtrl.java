package com.alidao.basic.web.control;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alidao.basic.entity.Advert;
import com.alidao.basic.service.AdvertService;
import com.alidao.jse.util.StringUtil;
import com.alidao.jxe.control.WebCtrl;
import com.alidao.jxe.help.PowerHelper;
import com.alidao.jxe.model.PageParam;

/**
 * 广告控制层，使用springMVC注入
 */
@Controller
@RequestMapping("advert")
public class AdvertCtrl extends WebCtrl {

	@Autowired
	private AdvertService advertService;
	
	
	@RequestMapping("init")
	public void init(Model model) {
	}
	
	@RequestMapping("list")
	public void list(PageParam pageParam, 
			Advert object, HttpServletResponse response) throws IOException {
				advertService.page(pageParam, object).jsonOut(response);
	}
	
	@RequestMapping("input")
	public void input(Model model, String id) {
		if (!StringUtil.isEmpty(id)) {
			model.addAttribute("object", advertService.find(id));
		}
	}
	
	
	@RequestMapping("save")
	public void save(Advert object, HttpServletResponse response) throws IOException {
			if (StringUtil.isEmpty(object.getId())) {
				object.setCreaterId(PowerHelper.get());
			}
			getResponse(advertService.save(object)).jsonOut(response);
	}
	
	
	@RequestMapping("lose/{id}")
	public void lose(
			@PathVariable("id") String id, 
			HttpServletResponse response) throws IOException {
		getResponse(advertService.lose(id)).jsonOut(response);
	}
	
}
