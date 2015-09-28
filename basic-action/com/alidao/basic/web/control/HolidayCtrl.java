package com.alidao.basic.web.control;

import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import com.alidao.basic.entity.Holiday;
import com.alidao.basic.service.HolidayService;
import com.alidao.jse.util.StringUtil;
import com.alidao.jxe.control.WebCtrl;
import com.alidao.jxe.model.PageParam;

@Controller
@RequestMapping("holiday")
public class HolidayCtrl extends WebCtrl {

	@Autowired
	private HolidayService holidayService;
	
	@RequestMapping("init")
	public void init() {
		
	}

	@RequestMapping("page")
	public void page(PageParam pageParam,
			Holiday object, HttpServletResponse response)
					throws Exception {
		holidayService.page(pageParam, object).jsonOut(response);
	}

	
	@RequestMapping("input")
	public void input(String id, Model model) {
		if(StringUtil.isNotBlank(id)) {
			model.addAttribute("object", holidayService.find(id));
		}
	}

	@RequestMapping("save")
	public void save(Holiday object,
			HttpServletResponse response)
			throws Exception {
		if (StringUtil.isEmpty(object.getId())) {
			getResponse(
					holidayService.saveBussiness(object)
			).jsonOut(response);
		} else {
			getResponse(
					holidayService.mdfyBussiness(object)
			).jsonOut(response);
		}
	}
	
	
	@RequestMapping("lose/{id}")
	public void lose(
			@PathVariable("id") String id, 
			HttpServletResponse response)
			throws Exception {
		getResponse(
				holidayService.lose(id)
		).jsonOut(response);
	}
	
	
}
