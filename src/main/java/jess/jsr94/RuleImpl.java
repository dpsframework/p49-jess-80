package jess.jsr94;

import jess.Defrule;

import javax.rules.admin.Rule;

class RuleImpl extends NameDescriptionProperties implements Rule {

    public RuleImpl(Defrule rule) {
        super(rule.getName(), rule.getDocstring());
    }

}
