/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.dynamic.data.mapping.form.evaluator.impl.internal;

import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderTracker;
import com.liferay.dynamic.data.mapping.expression.DDMExpressionFactory;
import com.liferay.dynamic.data.mapping.form.evaluator.DDMFormEvaluationException;
import com.liferay.dynamic.data.mapping.form.evaluator.DDMFormEvaluationResult;
import com.liferay.dynamic.data.mapping.form.evaluator.DDMFormEvaluator;
import com.liferay.dynamic.data.mapping.form.evaluator.DDMFormEvaluatorContext;
import com.liferay.dynamic.data.mapping.io.DDMFormValuesJSONDeserializer;
import com.liferay.dynamic.data.mapping.service.DDMDataProviderInstanceService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.service.UserLocalService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * @author Pablo Carvalho
 */
@Component(immediate = true, service = DDMFormEvaluator.class)
public class DDMFormEvaluatorImpl implements DDMFormEvaluator {

	@Override
	public DDMFormEvaluationResult evaluate(
			DDMFormEvaluatorContext ddmFormEvaluatorContext)
		throws DDMFormEvaluationException {

		try {
			DDMFormEvaluatorHelper ddmFormRuleEvaluatorHelper =
				new DDMFormEvaluatorHelper(
					_ddmDataProviderTracker, _ddmDataProviderInstanceService,
					_ddmExpressionFactory, ddmFormEvaluatorContext,
					_ddmFormValuesJSONDeserializer, _jsonFactory,
					_userLocalService);

			return ddmFormRuleEvaluatorHelper.evaluate();
		}
		catch (PortalException pe) {
			throw new DDMFormEvaluationException(pe);
		}
	}

	@Reference(
		cardinality = ReferenceCardinality.OPTIONAL,
		policy = ReferencePolicy.DYNAMIC,
		policyOption = ReferencePolicyOption.GREEDY
	)
	private volatile DDMDataProviderInstanceService
		_ddmDataProviderInstanceService;

	@Reference
	private DDMDataProviderTracker _ddmDataProviderTracker;

	@Reference
	private DDMExpressionFactory _ddmExpressionFactory;

	@Reference
	private DDMFormValuesJSONDeserializer _ddmFormValuesJSONDeserializer;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private UserLocalService _userLocalService;

}