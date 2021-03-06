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

package com.liferay.dynamic.data.mapping.type.text.internal;

import com.liferay.dynamic.data.mapping.data.provider.DDMDataProvider;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderContext;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderContextContributor;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderContextFactory;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderException;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderOutputParametersSettings;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderParameterSettings;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderRequest;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderResponse;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderTracker;
import com.liferay.dynamic.data.mapping.form.field.type.DDMFormFieldTemplateContextContributor;
import com.liferay.dynamic.data.mapping.model.DDMFormField;
import com.liferay.dynamic.data.mapping.model.DDMFormFieldOptions;
import com.liferay.dynamic.data.mapping.model.LocalizedValue;
import com.liferay.dynamic.data.mapping.model.Value;
import com.liferay.dynamic.data.mapping.render.DDMFormFieldRenderingContext;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.CharPool;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Marcellus Tavares
 */
@Component(
	immediate = true, property = "ddm.form.field.type.name=text",
	service = {
		TextDDMFormFieldTemplateContextContributor.class,
		DDMFormFieldTemplateContextContributor.class
	}
)
public class TextDDMFormFieldTemplateContextContributor
	implements DDMFormFieldTemplateContextContributor {

	@Override
	public Map<String, Object> getParameters(
		DDMFormField ddmFormField,
		DDMFormFieldRenderingContext ddmFormFieldRenderingContext) {

		Map<String, Object> parameters = new HashMap<>();

		parameters.put(
			"displayStyle",
			GetterUtil.getString(
				ddmFormField.getProperty("displayStyle"), "singleline"));

		LocalizedValue placeholder = (LocalizedValue)ddmFormField.getProperty(
			"placeholder");

		Locale locale = ddmFormFieldRenderingContext.getLocale();

		parameters.put("placeholder", getValueString(placeholder, locale));

		LocalizedValue tooltip = (LocalizedValue)ddmFormField.getProperty(
			"tooltip");

		parameters.put("tooltip", getValueString(tooltip, locale));

		List<Object> options = getOptions(
			ddmFormField, ddmFormFieldRenderingContext);

		parameters.put("autocomplete", !options.isEmpty());
		parameters.put("options", options);

		return parameters;
	}

	protected void addDDMDataProviderContextParameters(
		DDMDataProviderContext ddmDataProviderContext,
		DDMFormFieldRenderingContext ddmFormFieldRenderingContext) {

		if (ddmDataProviderContext.getType() == null) {
			return;
		}

		List<DDMDataProviderContextContributor>
			ddmDataProviderContextContributors =
				ddmDataProviderTracker.getDDMDataProviderContextContributors(
					ddmDataProviderContext.getType());

		HttpServletRequest request =
			ddmFormFieldRenderingContext.getHttpServletRequest();

		for (DDMDataProviderContextContributor
				ddmDataProviderContextContributor :
					ddmDataProviderContextContributors) {

			Map<String, String> parameters =
				ddmDataProviderContextContributor.getParameters(request);

			if (parameters == null) {
				continue;
			}

			ddmDataProviderContext.addParameters(parameters);
		}

		ddmDataProviderContext.addParameter(
			"filterParameterValue",
			String.valueOf(ddmFormFieldRenderingContext.getValue()));
	}

	protected DDMFormFieldOptions createDDMFormFieldOptions(
		DDMFormField ddmFormField,
		DDMFormFieldRenderingContext ddmFormFieldRenderingContext) {

		List<Map<String, String>> options =
			(List<Map<String, String>>)
				ddmFormFieldRenderingContext.getProperty("options");

		if (options.isEmpty()) {
			return ddmFormField.getDDMFormFieldOptions();
		}

		DDMFormFieldOptions ddmFormFieldOptions = new DDMFormFieldOptions();

		for (Map<String, String> option : options) {
			ddmFormFieldOptions.addOptionLabel(
				option.get("value"), ddmFormFieldRenderingContext.getLocale(),
				option.get("label"));
		}

		return ddmFormFieldOptions;
	}

	protected DDMFormFieldOptions createDDMFormFieldOptionsFromDataProvider(
		DDMFormField ddmFormField,
		DDMFormFieldRenderingContext ddmFormFieldRenderingContext) {

		DDMFormFieldOptions ddmFormFieldOptions = new DDMFormFieldOptions();

		ddmFormFieldOptions.setDefaultLocale(
			ddmFormFieldRenderingContext.getLocale());

		try {
			String ddmDataProviderInstanceId = GetterUtil.getString(
				ddmFormField.getProperty("ddmDataProviderInstanceId"));

			DDMDataProviderContext ddmDataProviderContext =
				ddmDataProviderContextFactory.create(ddmDataProviderInstanceId);

			ddmDataProviderContext.setHttpServletRequest(
				ddmFormFieldRenderingContext.getHttpServletRequest());

			addDDMDataProviderContextParameters(
				ddmDataProviderContext, ddmFormFieldRenderingContext);

			DDMDataProvider ddmDataProvider = getDDMDataProvider(
				ddmDataProviderContext);

			DDMDataProviderResponse ddmDataProviderResponse =
				executeDDMDataProvider(ddmDataProvider, ddmDataProviderContext);

			String ddmDataProviderInstanceOutput = GetterUtil.getString(
				ddmFormField.getProperty("ddmDataProviderInstanceOutput"));

			if (Validator.isNotNull(ddmDataProviderInstanceOutput)) {
				DDMDataProviderOutputParametersSettings outputParameterSetting =
					getDDMDataProviderOutputParametersSetting(
						ddmDataProviderInstanceOutput, ddmDataProvider,
						ddmDataProviderContext);

				String[] paths = StringUtil.split(
					outputParameterSetting.outputParameterPath(),
					CharPool.SEMICOLON);

				String key = paths[0];

				String value = key;

				if (paths.length > 1) {
					value = paths[1];
				}

				for (Map<Object, Object> map :
						ddmDataProviderResponse.getData()) {

					ddmFormFieldOptions.addOptionLabel(
						String.valueOf(map.get(value)),
						ddmFormFieldRenderingContext.getLocale(),
						String.valueOf(map.get(key)));
				}
			}
			else {
				for (Map<Object, Object> ddmDataProviderData :
						ddmDataProviderResponse.getData()) {

					for (Entry<Object, Object> entry :
							ddmDataProviderData.entrySet()) {

						ddmFormFieldOptions.addOptionLabel(
							String.valueOf(entry.getValue()),
							ddmFormFieldRenderingContext.getLocale(),
							String.valueOf(entry.getKey()));
					}
				}
			}
		}
		catch (Exception e) {
			if (_log.isDebugEnabled()) {
				_log.debug(e, e);
			}
		}

		return ddmFormFieldOptions;
	}

	protected DDMDataProviderResponse executeDDMDataProvider(
			DDMDataProvider ddmDataProvider,
			DDMDataProviderContext ddmDataProviderContext)
		throws DDMDataProviderException {

		DDMDataProviderRequest ddmDataProviderRequest =
			new DDMDataProviderRequest(ddmDataProviderContext);

		return ddmDataProvider.getData(ddmDataProviderRequest);
	}

	protected DDMDataProvider getDDMDataProvider(
		DDMDataProviderContext ddmDataProviderContext) {

		String type = ddmDataProviderContext.getType();

		if (type == null) {
			return ddmDataProviderTracker.getDDMDataProviderByInstanceId(
				ddmDataProviderContext.getDDMDataProviderInstanceId());
		}

		return ddmDataProviderTracker.getDDMDataProvider(type);
	}

	protected DDMDataProviderOutputParametersSettings
		getDDMDataProviderOutputParametersSetting(
			String ddmDataProviderOutput, DDMDataProvider ddmDataProvider,
			DDMDataProviderContext ddmDataProviderContext) {

		DDMDataProviderParameterSettings ddmDataProviderParemeterSettings =
			(DDMDataProviderParameterSettings)
				ddmDataProviderContext.getSettingsInstance(
					ddmDataProvider.getSettings());

		for (DDMDataProviderOutputParametersSettings
				ddmDataProviderOutputParametersSetting :
					ddmDataProviderParemeterSettings.outputParameters()) {

			if (ddmDataProviderOutput.equals(
					ddmDataProviderOutputParametersSetting.
						outputParameterName())) {

				return ddmDataProviderOutputParametersSetting;
			}
		}

		return null;
	}

	protected DDMFormFieldOptions getDDMFormFieldOptions(
		DDMFormField ddmFormField,
		DDMFormFieldRenderingContext ddmFormFieldRenderingContext) {

		String dataSourceType = GetterUtil.getString(
			ddmFormField.getProperty("dataSourceType"), "manual");

		if (Objects.equals(dataSourceType, "data-provider")) {
			return createDDMFormFieldOptionsFromDataProvider(
				ddmFormField, ddmFormFieldRenderingContext);
		}
		else {
			return createDDMFormFieldOptions(
				ddmFormField, ddmFormFieldRenderingContext);
		}
	}

	protected List<Object> getOptions(
		DDMFormField ddmFormField,
		DDMFormFieldRenderingContext ddmFormFieldRenderingContext) {

		TextDDMFormFieldContextHelper selectDDMFormFieldContextHelper =
			new TextDDMFormFieldContextHelper(
				getDDMFormFieldOptions(
					ddmFormField, ddmFormFieldRenderingContext),
				ddmFormFieldRenderingContext.getLocale());

		return selectDDMFormFieldContextHelper.getOptions();
	}

	protected String getValueString(Value value, Locale locale) {
		if (value != null) {
			return value.getString(locale);
		}

		return StringPool.BLANK;
	}

	@Reference
	protected DDMDataProviderContextFactory ddmDataProviderContextFactory;

	@Reference
	protected DDMDataProviderTracker ddmDataProviderTracker;

	@Reference
	protected JSONFactory jsonFactory;

	private static final Log _log = LogFactoryUtil.getLog(
		TextDDMFormFieldTemplateContextContributor.class);

}