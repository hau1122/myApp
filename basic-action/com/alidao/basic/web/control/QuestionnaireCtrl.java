package com.alidao.basic.web.control;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import com.alidao.basic.entity.Questionnaire;
import com.alidao.basic.service.QuestionnaireService;
import com.alidao.jse.util.StringUtil;
import com.alidao.jxe.control.WebCtrl;
import com.alidao.jxe.model.PageParam;
import com.alidao.jxe.util.HttpUtil;

@Controller
@RequestMapping("questionnaire")
public class QuestionnaireCtrl extends WebCtrl {

	@Autowired
	private QuestionnaireService questionnaireService;
	
	@RequestMapping("init")
	public void init(Model model,HttpServletRequest request) {
		model.addAttribute("webapp", HttpUtil.getWebAppUrl(request));
	}

	@RequestMapping("page")
	public void page(PageParam pageParam, 
			Questionnaire object, HttpServletResponse response) 
					throws Exception {
		questionnaireService.page(pageParam, object).jsonOut(response);
	}


	@RequestMapping("input")
	public void edit(String id, Model model) {
		if (id != null) {
			Questionnaire object = questionnaireService.find(id);
			model.addAttribute("object", object);
		}
	}

	@RequestMapping("save")
	public void save(
			HttpServletResponse response, 
			Questionnaire object)
			throws Exception {
		if (StringUtil.isEmpty(object.getId())) {
			getResponse(
				questionnaireService.save(object)
			).jsonOut(response);
		} else {
			getResponse(
				questionnaireService.mdfy(object)
			).jsonOut(response);
		}
	}
	
	@RequestMapping("lose/{id}")
	public void lose(
			@PathVariable("id") String id, 
			HttpServletResponse response)
			throws Exception {
		getResponse(
			questionnaireService.lose(id)
		).jsonOut(response);
	}
}
