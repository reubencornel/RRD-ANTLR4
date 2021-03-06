/*
 * @cond LICENSE
 * ######################################################################################
 * # LGPL License                                                                       #
 * #                                                                                    #
 * # This file is part of the RRD-AntLR4                                                #
 * # Copyright (c) 2016-17, Philipp Kraus (philipp.kraus@flashpixx.de)                  #
 * # This program is free software: you can redistribute it and/or modify               #
 * # it under the terms of the GNU Lesser General Public License as                     #
 * # published by the Free Software Foundation, either version 3 of the                 #
 * # License, or (at your option) any later version.                                    #
 * #                                                                                    #
 * # This program is distributed in the hope that it will be useful,                    #
 * # but WITHOUT ANY WARRANTY; without even the implied warranty of                     #
 * # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                      #
 * # GNU Lesser General Public License for more details.                                #
 * #                                                                                    #
 * # You should have received a copy of the GNU Lesser General Public License           #
 * # along with this program. If not, see http://www.gnu.org/licenses/                  #
 * ######################################################################################
 * @endcond
 */

package de.flashpixx.rrd_antlr4.antlr;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;


/**
 * lexer adaptor for parsing AntLR grammar files
 *
 * @see https://github.com/antlr/grammars-v4/blob/master/antlr4/LexerAdaptor.py
 */
public abstract class IAntLRv4LexerAdaptor extends Lexer
{

    /**
     * Track whether we are inside of a rule and whether it is lexical parser. _currentRuleType==Token.INVALID_TYPE
     * means that we are outside of a rule. At the first sign of a rule name reference and _currentRuleType==invalid, we
     * can assume that we are starting a parser rule. Similarly, seeing a token reference when not already in rule means
     * starting a token rule. The terminating ';' of a rule, flips this back to invalid type.
     *
     * This is not perfect logic but works. For example, "grammar T;" means that we start and stop a lexical rule for
     * the "T;". Dangerous but works.
     *
     * The whole point of this state information is to distinguish between [..arg actions..] and [charsets]. Char sets
     * can only occur in lexical rules and arg actions cannot occur.
     */
    private int _currentRuleType = Token.INVALID_TYPE;

    public IAntLRv4LexerAdaptor( CharStream input )
    {
        super( input );
    }

    public int getCurrentRuleType()
    {
        return _currentRuleType;
    }

    public void setCurrentRuleType( int ruleType )
    {
        this._currentRuleType = ruleType;
    }

    @Override
    public Token emit()
    {
        if ( _type == ANTLRv4Lexer.ID )
        {
            String firstChar = _input.getText( Interval.of( _tokenStartCharIndex, _tokenStartCharIndex ) );
            if ( Character.isUpperCase( firstChar.charAt( 0 ) ) )
            {
                _type = ANTLRv4Lexer.TOKEN_REF;
            }
            else
            {
                _type = ANTLRv4Lexer.RULE_REF;
            }

            if ( _currentRuleType == Token.INVALID_TYPE )
            { // if outside of rule def
                _currentRuleType = _type; // set to inside lexer or parser rule
            }
        }
        else if ( _type == ANTLRv4Lexer.SEMI )
        { // exit rule def
            _currentRuleType = Token.INVALID_TYPE;
        }

        return super.emit();
    }

    protected void handleBeginArgument()
    {
        if ( inLexerRule() )
        {
            pushMode( ANTLRv4Lexer.LexerCharSet );
            more();
        }
        else
        {
            pushMode( ANTLRv4Lexer.Argument );
        }
    }

    protected void handleEndArgument()
    {
        popMode();
        if ( _modeStack.size() > 0 )
        {
            setType( ANTLRv4Lexer.ARGUMENT_CONTENT );
        }
    }

    protected void handleEndAction()
    {
        popMode();
        if ( _modeStack.size() > 0 )
        {
            setType( ANTLRv4Lexer.ACTION_CONTENT );
        }
    }

    private boolean inLexerRule()
    {
        return _currentRuleType == ANTLRv4Lexer.TOKEN_REF;
    }

    @SuppressWarnings( "unused" )
    private boolean inParserRule()
    { // not used, but added for clarity
        return _currentRuleType == ANTLRv4Lexer.RULE_REF;
    }
}