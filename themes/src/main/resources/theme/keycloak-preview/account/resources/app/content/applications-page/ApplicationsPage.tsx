/* 
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import * as React from 'react';
import { AxiosResponse } from 'axios';

import {
  DataList,
  DataListItem,
  DataListItemRow,
  DataListCell,
  DataListToggle,
  DataListContent,
  DataListItemCells,
  Text,
  TextVariants,
  Grid,
  GridItem
} from '@patternfly/react-core';

import { InfoAltIcon, CheckIcon, LinkIcon, BuilderImageIcon } from '@patternfly/react-icons';
import { ContentPage } from '../ContentPage';
import { ContinueCancelModal } from '../../widgets/ContinueCancelModal';
import { AccountServiceClient } from '../../account-service/account.service';

export interface ApplicationsPageProps {
}

export interface ApplicationsPageState {
  isRowOpen: boolean[];
  applications: Application[];
}

interface Application {
  clientId: string;
  clientName: string;
  description: string;
  url: string;
  internal: boolean;
  inUse: boolean;
  scopes: string[];
  createdDate: number;
}

export class ApplicationsPage extends React.Component<ApplicationsPageProps, ApplicationsPageState> {

  public constructor(props: ApplicationsPageProps) {
    super(props);
    this.state = {
      isRowOpen: [],
      applications: []
    };

    this.fetchApplications();
  }

  private removeConsent = (clientId: string) => {
    AccountServiceClient.Instance.doDelete("/applications/" + clientId + "/consent")
      .then(() => {
        this.fetchApplications();
      });
    //DELETE /{realm}/users/{id}/consents/{client}
  }

  private onToggle = (row: number): void => {
    const newIsRowOpen: boolean[] = this.state.isRowOpen;
    newIsRowOpen[row] = !newIsRowOpen[row];
    this.setState({ isRowOpen: newIsRowOpen });
  };

  private fetchApplications(): void {
    AccountServiceClient.Instance.doGet("/applications")
      .then((response: AxiosResponse<Object>) => {
        const applications = response.data as Application[];
        this.setState({
          isRowOpen: this.collapseRows(applications),
          applications: applications
        });
      });
  }

  private collapseRows(applications: Application[]): boolean[] {
    const openRows: boolean[] = new Array<boolean>().fill(true);
    applications.forEach((application: Application, appIndex: number) => {
      openRows[appIndex] = false;
    });
    return openRows;
  }

  //TODO do not render null
  private renderScopes(scopes: string[]): void {
    if (scopes) {
      scopes.forEach((name: string, appIndex: number) => {
        <GridItem span={10}><CheckIcon />name</GridItem>
      });
    }
  }

  public render(): React.ReactNode {
    return (
      <ContentPage title="applications">
        <DataList aria-label="device-activity">
          {this.state.applications.map((application: Application, appIndex: number) => {
            return (
              <DataListItem key={'application-' + appIndex} aria-labelledby="simple-item2" isExpanded={this.state.isRowOpen[appIndex]}>
                <DataListItemRow>
                  <DataListToggle
                    onClick={() => this.onToggle(appIndex)}
                    isExpanded={this.state.isRowOpen[appIndex]}
                    id={'applicationToggle' + appIndex}
                    aria-controls="ex-expand1"
                  />
                  <DataListItemCells
                    dataListCells={[
                      <DataListCell key={'app-' + appIndex}>
                        <BuilderImageIcon /> <Text component={TextVariants.small}>{application.clientId}</Text>
                      </DataListCell>,
                      <DataListCell key={'internal-' + appIndex}>
                        <span>{application.internal ? 'Internal' : 'Third-party'}</span>
                      </DataListCell>,
                      <DataListCell key={'status-' + appIndex}>
                        <span>{application.inUse ? 'In-use' : ''}</span>
                      </DataListCell>,
                      <DataListCell key={'baseUrl-' + appIndex}>
                        <a href={application.url}><LinkIcon />{application.url}</a>
                      </DataListCell>,
                    ]}
                  />
                </DataListItemRow>
                <DataListContent
                  noPadding={false}
                  aria-label="Application Details"
                  id="ex-expand1"
                  isHidden={!this.state.isRowOpen[appIndex]}
                >
                  <Grid gutter="sm" sm={12} md={12} lg={12}>
                    <GridItem>{'Client: ' + application.clientId}</GridItem>
                    <GridItem>{'Description: ' + application.description}</GridItem>
                    <GridItem>{'URL: ' + application.url}</GridItem>
                    {application.scopes &&
                      <React.Fragment>
                        <GridItem span={12}>
                          Has access to:
                        </GridItem>
                        {application.scopes.map((scope: string, scopeIndex: number) => {
                          return (
                            <React.Fragment key={'scope-' + scopeIndex} >
                              <GridItem span={12}><CheckIcon /> {scope}</GridItem>
                            </React.Fragment>
                          )
                        })}
                        <GridItem>{'Access granted on: '}
                          {new Intl.DateTimeFormat('en-US', {
                            year: 'numeric',
                            month: 'long',
                            day: 'numeric',
                            hour: 'numeric',
                            minute: 'numeric',
                            second: 'numeric'
                          }).format(application.createdDate)}
                        </GridItem>
                        <hr />
                        <GridItem span={2} rowSpan={1}>
                          <React.Fragment>
                            <ContinueCancelModal
                              buttonTitle='Remove' // required
                              buttonVariant='secondary' // defaults to 'primary'
                              modalTitle='Remove Access' // required
                              modalMessage='This will remove the currently granted access permission for AdminCLI. You will need to grant access again if you want ot user this app.'
                              modalContinueButtonLabel='Confirm' // defaults to 'Continue'
                              modalCancelButtonLabel='Cancel' // defaults to 'Cancel'
                              onContinue={() => this.removeConsent(application.clientId)} // required
                            />
                          </React.Fragment>
                        </GridItem>
                        <GridItem span={10}>By clicking ‘Remove Access’, you will remove granted permissions of this app. This app will no longer use your information.</GridItem>
                      </React.Fragment>
                    }
                  </Grid>
                </DataListContent>
              </DataListItem>
            )
          })}
        </DataList>
      </ContentPage>
    );
  }
};
