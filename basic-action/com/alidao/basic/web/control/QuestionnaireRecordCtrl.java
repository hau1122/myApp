package com.alidao.basic.web.control;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import com.alidao.basic.entity.QuestionnaireRecord;
import com.alidao.basic.service.QuestionnaireRecordService;
import com.alidao.jse.util.StringUtil;
import com.alidao.jxe.control.WebCtrl;
import com.alidao.jxe.model.PageParam;

@Controller
@RequestMapping("questionnaireRecord")
public class QuestionnaireRecordCtrl extends WebCtrl {

	@Autowired
	private QuestionnaireRecordService questionnaireRecordService;
	
	@RequestMapping("init")
	public void init(String questionnaireId,Model model) {
		model.addAttribute("questionnaireId",questionnaireId);
	}
	
	@RequestMapping("detail")
	public void detail(String questionnaireId,String userId,Model model) {
		model.addAttribute("questionnaireId",questionnaireId);
		model.addAttribute("userId",userId);
	}
	
	@RequestMapping("topicDetail")
	public void topicDetail(String topicId,Model model) {
		model.addAttribute("topicId",topicId);
	}
	
	@RequestMapping("page")
	public void page(PageParam pageParam, 
			QuestionnaireRecord object, HttpServletResponse response) 
					throws Exception {
		if(StringUtil.isEmpty(object.getUserAccount())){
			object.setUserAccount(null);
		}
		if(!StringUtil.isEmpty(object.getGroupBy())){
			object.setGroupBy(null);
			object.addGroupBy("user_id");
		}
		questionnaireRecordService.page(pageParam, object).jsonOut(response);
	}


	@RequestMapping("input")
	public void edit(String id, Model model) {
		if (id != null) {
			QuestionnaireRecord object = questionnaireRecordService.find(id);
			model.addAttribute("object", object);
		}
	}

	@RequestMapping("save")
	public void save(
			HttpServletResponse response, 
			QuestionnaireRecord object)
			throws Exception {
		if (StringUtil.isEmpty(object.getId())) {
			getResponse(
				questionnaireRecordService.save(object)
			).jsonOut(response);
		} else {
			getResponse(
				questionnaireRecordService.mdfy(object)
			).jsonOut(response);
		}
	}
	
	@RequestMapping("lose/{id}")
	public void lose(
			@PathVariable("id") String id, 
			HttpServletResponse response)
			throws Exception {
		getResponse(
			questionnaireRecordService.lose(id)
		).jsonOut(response);
	}
}
