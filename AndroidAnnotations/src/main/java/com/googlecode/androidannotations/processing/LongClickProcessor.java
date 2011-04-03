/*
 * Copyright 2010-2011 Pierre-Yves Ricau (py.ricau at gmail.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.googlecode.androidannotations.processing;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import com.googlecode.androidannotations.annotations.Id;
import com.googlecode.androidannotations.annotations.LongClick;
import com.googlecode.androidannotations.generation.LongClickInstruction;
import com.googlecode.androidannotations.model.Instruction;
import com.googlecode.androidannotations.model.MetaActivity;
import com.googlecode.androidannotations.model.MetaModel;
import com.googlecode.androidannotations.rclass.IRClass;
import com.googlecode.androidannotations.rclass.IRInnerClass;
import com.googlecode.androidannotations.rclass.RClass.Res;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JVar;

/**
 * @author Benjamin Fellous
 * @author Pierre-Yves Ricau
 */
public class LongClickProcessor implements ElementProcessor {

	private final IRClass rClass;

	public LongClickProcessor(IRClass rClass) {
		this.rClass = rClass;
	}

	@Override
	public Class<? extends Annotation> getTarget() {
		return LongClick.class;
	}

	@Override
	public void process(Element element, MetaModel metaModel) {

		String methodName = element.getSimpleName().toString();

		LongClick annotation = element.getAnnotation(LongClick.class);
		int idValue = annotation.value();

		IRInnerClass rInnerClass = rClass.get(Res.ID);
		String longClickQualifiedId;

		if (idValue == Id.DEFAULT_VALUE) {
			String fieldName = element.getSimpleName().toString();
			int lastIndex = fieldName.lastIndexOf("LongClicked");
			if (lastIndex != -1) {
				fieldName = fieldName.substring(0, lastIndex);
			}
			longClickQualifiedId = rInnerClass.getIdQualifiedName(fieldName);
		} else {
			longClickQualifiedId = rInnerClass.getIdQualifiedName(idValue);
		}

		Element enclosingElement = element.getEnclosingElement();
		MetaActivity metaActivity = metaModel.getMetaActivities().get(enclosingElement);
		List<Instruction> onCreateInstructions = metaActivity.getOnCreateInstructions();

		ExecutableElement executableElement = (ExecutableElement) element;
		List<? extends VariableElement> parameters = executableElement.getParameters();

		boolean viewParameter = parameters.size() == 1;

		TypeMirror returnType = executableElement.getReturnType();

		boolean returnMethodResult = returnType.getKind() != TypeKind.VOID;
		
		Instruction instruction = new LongClickInstruction(methodName, longClickQualifiedId, viewParameter, returnMethodResult);
		onCreateInstructions.add(instruction);

	}


    @Override
    public void process(Element element, JCodeModel codeModel, ActivitiesHolder activitiesHolder) {
        ActivityHolder holder = activitiesHolder.getActivityHolder(element);

        String methodName = element.getSimpleName().toString();

        ExecutableElement executableElement = (ExecutableElement) element;
        List<? extends VariableElement> parameters = executableElement.getParameters();
        TypeMirror returnType = executableElement.getReturnType();
        boolean returnMethodResult = returnType.getKind() != TypeKind.VOID;

        boolean hasItemParameter = parameters.size() == 1;
        
        JFieldRef idRef = extractClickQualifiedId(element, codeModel);

        JDefinedClass listenerClass = codeModel.anonymousClass(codeModel.ref("android.view.View.OnLongClickListener"));
        JMethod listenerMethod = listenerClass.method(JMod.PUBLIC, codeModel.BOOLEAN, "onLongClick");
        JClass viewClass = codeModel.ref("android.view.View");
        
        JVar viewParam = listenerMethod.param(viewClass, "view");
        
        JBlock listenerMethodBody = listenerMethod.body();
        
        JInvocation call = JExpr.invoke(methodName);
        
        if (returnMethodResult) {
            listenerMethodBody._return(call);
        } else {
            listenerMethodBody.add(call);
            listenerMethodBody._return(JExpr.TRUE);
        }

        if (hasItemParameter) {
            call.arg(viewParam);
        }

        JBlock body = holder.afterSetContentView.body();

        body.add(JExpr.invoke(JExpr.invoke("findViewById").arg(idRef),"setOnLongClickListener").arg(JExpr._new(listenerClass)));
    }
    
    private JFieldRef extractClickQualifiedId(Element element, JCodeModel codeModel) {
        LongClick annotation = element.getAnnotation(LongClick.class);
        int idValue = annotation.value();
        IRInnerClass rInnerClass = rClass.get(Res.ID);
        if (idValue == Id.DEFAULT_VALUE) {
            String fieldName = element.getSimpleName().toString();
            int lastIndex = fieldName.lastIndexOf("LongClicked");
            if (lastIndex != -1) {
                fieldName = fieldName.substring(0, lastIndex);
            }
            return rInnerClass.getIdStaticRef(fieldName, codeModel);

        } else {
            return rInnerClass.getIdStaticRef(idValue, codeModel);
        }
    }

}
