package com.sokeeper.service.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.fastjson.JSON;
import com.sokeeper.domain.subject.SubjectEntity;
import com.sokeeper.service.SubjectKeywordService;
import com.sokeeper.service.support.ResourceHelper;
import com.sokeeper.service.support.SubjectKeywordServiceImpl;
import com.sokeeper.web.dto.MovieDto;

public class QueryServlet extends HttpServlet {
	private SubjectKeywordService subjectKeywordService = SubjectKeywordServiceImpl
			.getInstance();
	private static final long serialVersionUID = -9140179114615474818L;

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String query = request.getParameter("words");
		String result = "";
		List<MovieDto> movies = new ArrayList<MovieDto>();
		if (query != null) {
			List<SubjectEntity> subjects = subjectKeywordService.search(query,
					0, 40);
			for (int i = 0; i < subjects.size(); i++) {
				SubjectEntity entity = subjects.get(i);
				MovieDto movie = new MovieDto();
				movie.setName(entity.getName());
				movie.setInfo(entity.getInfo());
				movie.setImageUrl(ResourceHelper.getInstance().getEnv(
						ResourceHelper.IMAGE_HTTP_SERVER)
						+ entity.getExternalId() + ".jpg");
				movie.setKeywordCountList(entity.getKeywordCountList());
				movie.setScore(entity.getScore());
				movie.setSummary(entity.getSummary());
				movie.setSubjectId(entity.getExternalId());
				movies.add(movie);
			}
			result = JSON.toJSONString(movies);
		}
		PrintWriter out = response.getWriter();

		out.println(result);

	}
}
