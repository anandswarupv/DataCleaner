package org.datacleaner.visualization

import org.datacleaner.api.Renderer
import org.datacleaner.api.RendererBean
import org.datacleaner.api.RendererPrecedence
import org.datacleaner.api.RendererBean
import org.datacleaner.result.html.HtmlFragment
import org.datacleaner.result.renderer.HtmlRenderingFormat
import org.datacleaner.result.html.HtmlRenderingContext
import org.datacleaner.result.html.SimpleHtmlFragment
import org.datacleaner.result.renderer.HtmlRenderingFormat
import org.datacleaner.result.html.HtmlRenderer

@RendererBean(classOf[HtmlRenderingFormat])
class ScatterAnalyzerResultHtmlRenderer extends HtmlRenderer[ScatterAnalyzerResult] {

  override def handleFragment(frag: SimpleHtmlFragment, result: ScatterAnalyzerResult, context: HtmlRenderingContext) {
    val elementId = context.createElementId()

    frag.addHeadElement(ScatterAnalyzerResuableChartHeadElement)
    frag.addHeadElement(new ScatterAnalyzerChartScriptHeadElement(result, elementId));

    val html =
      <div class="scatterAnalyzerDiv">
        <div class="scatterChart" id={ elementId }>
        </div>
      </div>

    frag.addBodyElement(html.toString)
  }
}
