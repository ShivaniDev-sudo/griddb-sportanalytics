package mycode.controller;



import mycode.service.MatchEventsService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
public class MatchEventsController {

  @Autowired
  private MatchEventsService matchEventsService;

  @GetMapping("/pass-accuracy/{playerName}")
  public String getPassCountEveryFiveMin(@PathVariable String playerName, Model model) {
    Map<Integer, Integer> passCountByFiveMin = matchEventsService.getPassCountByFiveMin(playerName);

    // Prepare data for the chart
    model.addAttribute("playerName", playerName);
    model.addAttribute("timeIntervals", passCountByFiveMin.keySet().stream().sorted().toList());
    model.addAttribute("passCounts", passCountByFiveMin.values());

    return "pass-accuracy-graph"; 
  }
}
