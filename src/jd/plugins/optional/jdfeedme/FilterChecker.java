package jd.plugins.optional.jdfeedme;

import java.util.ArrayList;

public class FilterChecker 
{
	private ArrayList<Filter> filters;
	
	public FilterChecker(String filters_str)
	{
		// init filters
		filters = new ArrayList<Filter>();
		
		// parse the filter list
		filters_str = preProcessFilterString(filters_str);
		
		// go over all the filters
		for (String filter_str : filters_str.split("\n"))
		{
			if (filter_str.trim().length() == 0) continue;
			Filter filter = new Filter(filter_str.trim());
			filters.add(filter);
		}
	}
	
	// check if a target string matches one of the filters
	public boolean match(String target)
	{
		// make sure our filter list isn't empty (if so then nothing matched)
		if (filters.size() == 0) return false;
		
		// first preprocess the target
		target = preProcessTargetString(target);
		
		// go over all the filters
		for (Filter filter : filters)
		{
			// one good match is all we need
			if (filter.match(target)) return true;
		}
		
		// if here then none matched
		return false;
	}
	
	private String preProcessTargetString(String str)
	{
		str = str.toLowerCase();
		str = str.replaceAll("[^a-z0-9]+"," ");
		str = str.replaceAll("\\s+", " ");
		str = str.trim();
		return str;
	}
	
	private String preProcessFilterString(String str)
	{
		str = str.replace("\\r", "");
		return str;
	}
}

class Filter
{
	private ArrayList<FilterToken> tokens;
	
	public Filter(String filter_str)
	{
		// parse the filter string
		filter_str = preProcessFilterString(filter_str);
		
		// tokenize the string
		tokens = tokenize(filter_str);
	}
	
	// assumes text is preprocessed, true if token matches the text
	public boolean match(String target)
	{
		// make sure all tokens match
		for (FilterToken token : tokens)
		{
			// one bad match enough to kill the entire filter
			if (!token.match(target)) return false;
		}
		
		// if here than all matched
		return true;
	}
	
	private String preProcessFilterString(String str)
	{
		str = str.toLowerCase();
		str = str.replaceAll("[^a-z0-9\\\"\\+\\-]+"," ");
		str = str.replaceAll("\\s+", " ");
		str = str.trim();
		return str;
	}
	
	private ArrayList<FilterToken> tokenize(String tokens_str)
	{
		ArrayList<FilterToken> result = new ArrayList<FilterToken>();
		boolean inside = false;
		boolean exact_negative_prefix = false;
		String current = "";
		String tokens_str_arr[] = tokens_str.split("((?<=[\\+\\-]?\\\")|(?=[\\+\\-]?\\\"))");
		for (int i=0; i<tokens_str_arr.length; i++)
		{
			String token_str = tokens_str_arr[i];
			
			// look for - before "exact phrases"
			if (token_str.equals("-"))
			{
				if (!inside)
				{
					// check if next token in "exact phrase"
					if ((i+1<tokens_str_arr.length) && tokens_str_arr[i+1].equals("\""))
					{
						exact_negative_prefix = true;
						continue;
					}
				}
			}
			
			// look for "exact phrases"
			if (token_str.equals("\""))
			{
				if (!inside) // next token is a new "phrase"
				{
					// add the previous token (current) which is before the "phrase"
					tokenizeAdd(result, current, false, false);
					inside = true;
				}
				else // current "phrase" token is terminated 
				{
					// add the "phrase" token
					tokenizeAdd(result, current, true, exact_negative_prefix);
					exact_negative_prefix = false;
					inside = false;
				}
				
				current = "";
				continue;
			}
			
			current = token_str;
		}
		
		// after the for, handle the last token
		tokenizeAdd(result, current, inside, exact_negative_prefix);
		
		// all done
		return result;
	}
	
	private void tokenizeAdd(ArrayList<FilterToken> to, String what, boolean is_exact, boolean is_negative)
	{
		// don't add empty tokens
		if (what.length() == 0) return;
		
		// add exact tokens
		if (is_exact)
		{
			FilterToken token = new FilterToken(what,is_exact,is_negative);
			to.add(token);
			return;
		}
		
		// add non-exact tokens, more parsing needed
		String[] words = what.split(" ");
		for (String word : words)
		{
			FilterToken token;
			
			// check if word starts with +
			if (word.startsWith("+")) 
			{
				if (word.length() > 1)
				{
					token = new FilterToken(word.substring(1),false,false);
					to.add(token);
				}
				continue;
			}
			
			// check if word starts with -
			if (word.startsWith("-")) 
			{
				if (word.length() > 1)
				{
					token = new FilterToken(word.substring(1),false,true);
					to.add(token);
				}
				continue;
			}
			
			// just a regular word without a prefix
			if (word.length() > 0)
			{
				token = new FilterToken(word,false,false);
				to.add(token);
			}
		}
	}
}

class FilterToken
{
	public String text;			// the text of the token
	public boolean is_exact; 	// does have " " around it
	public boolean is_negative; // does have - in the beginning
	
	public FilterToken(String text, boolean is_exact, boolean is_negative)
	{
		this.text = text;
		this.is_exact = is_exact;
		this.is_negative = is_negative;
	}
	
	// assumes text is preprocessed, true if token matches the text
	public boolean match(String target)
	{
		// first check if the text is found in target
		boolean found = false;
		if (is_exact)
		{
			// handle exact phrases
			/// add (?s) in the beginning of regexp if we need to match across different lines
			found = target.matches(".*\\b\\Q"+this.text+"\\E\\b.*");
		}
		else
		{
			// handle non-exact phrases
			/// add (?s) in the beginning of regexp if we need to match across different lines
			found = target.matches(".*\\b\\Q"+this.text+"\\E.*");
		}
		
		// return according to negative (xor)
		/// found and negative - false
		/// found and not negative - true
		/// not found and negative - true
		/// not found and not negative - false
		return found ^ is_negative;
	}
}
