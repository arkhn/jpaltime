package ca.uhn.fhir.jpa.starter.elasticsearch;

// TODO move to own package?

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

public class MyElasticsearchAnalysisConfigurer implements ElasticsearchAnalysisConfigurer{

	@Override
	public void configure(ElasticsearchAnalysisConfigurationContext theConfigCtx) {

		theConfigCtx.analyzer("autocompleteEdgeAnalyzer").custom()
			.tokenizer("pattern_all")
			.tokenFilters("lowercase", "stop", "edgengram_3_50");

		theConfigCtx.tokenizer("pattern_all")
			.type("pattern")
			.param("pattern", "(.*)")
			.param("group", "1");

		theConfigCtx.tokenFilter("edgengram_3_50")
			.type("edgeNGram")
			.param("min_gram", "3")
			.param("max_gram", "50");


		theConfigCtx.analyzer("autocompleteWordEdgeAnalyzer").custom()
			.tokenizer("standard")
			.tokenFilters("lowercase", "stop", "wordedgengram_3_50");

		theConfigCtx.tokenFilter("wordedgengram_3_50")
			.type("edgeNGram")
			.param("min_gram", "3")
			.param("max_gram", "20");

		theConfigCtx.analyzer("autocompletePhoneticAnalyzer").custom()
			.tokenizer("standard")
			.tokenFilters("stop", "snowball_english");

		theConfigCtx.tokenFilter("snowball_english")
			.type("snowball")
			.param("language", "English");

		theConfigCtx.analyzer("autocompleteNGramAnalyzer").custom()
			.tokenizer("standard")
			.tokenFilters("word_delimiter", "lowercase", "ngram_3_20");

		theConfigCtx.tokenFilter("ngram_3_20")
			.type("nGram")
			.param("min_gram", "3")
			.param("max_gram", "20");


		theConfigCtx.analyzer("standardAnalyzer").custom()
			.tokenizer("keyword");

		theConfigCtx.analyzer("exactAnalyzer")
			.custom()
			.tokenizer("keyword");

		theConfigCtx.analyzer("conceptParentPidsAnalyzer").custom()
			.tokenizer("whitespace");

		theConfigCtx.analyzer("termConceptPropertyAnalyzer").custom()
			.tokenizer("whitespace");
	}
}
