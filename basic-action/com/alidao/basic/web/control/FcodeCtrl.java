package com.alidao.basic.web.control;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alidao.basic.entity.Fcode;
import com.alidao.basic.service.FcodeService;
import com.alidao.jse.util.StringUtil;
import com.alidao.jxe.control.WebCtrl;
import com.alidao.jxe.model.PageParam;
import com.alidao.jxe.model.ParamException;

@Controller
@RequestMapping("fcode")
public class FcodeCtrl extends WebCtrl {

	@Autowired
	private FcodeService fcodeService;

	@RequestMapping("init")
	public void init() {
	}

	@RequestMapping("list")
	public void list(PageParam pageParam, Fcode object,
			HttpServletResponse response) throws IOException {
		fcodeService.page(pageParam, object).jsonOut(response);
	}

	@RequestMapping("edit")
	public void edit(Model model, String id) {
		if (StringUtil.isNotBlank(id)) {
			model.addAttribute("object", fcodeService.find(id));
		}
	}

	@RequestMapping("save")
	public void save(Fcode object, Integer nums, HttpServletResponse response)
			throws IOException, ParamException {
		if (StringUtil.isEmpty(object.getId())) {
			getResponse(fcodeService.save(object, nums)).jsonOut(response);
		} else {
			getResponse(fcodeService.mdfy(object)).jsonOut(response);
		}
	}
	
	@RequestMapping("lose/{id}")
	public void lose(@PathVariable("id") String id, 
			HttpServletResponse response) throws IOException {
		getResponse(fcodeService.lose(id)).jsonOut(response);
	}
}
