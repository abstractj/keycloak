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
  GridItem,
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

export interface GrantedScope {
  displayTest: string;
  id: string;
  name: string;
}

export interface Consent {
  createDate: number;
  grantedScopes: GrantedScope[];
  lastUpdatedDate: number;
}

interface Application {
  baseUrl: string;
  clientId: string;
  clientName: string;
  consent: Consent;
  description: string;
  inUse: boolean;
  offlineAccess: boolean;
  userConsentRequired: boolean;
  scope: string[];
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

  public render(): React.ReactNode {
    return (
      <ContentPage title="applications">
        <DataList id="applications-list" aria-label="Applications">
          {this.state.applications.map((application: Application, appIndex: number) => {
            return (
              <DataListItem key={'application-' + appIndex} aria-labelledby="applications-list" isExpanded={this.state.isRowOpen[appIndex]}>
                <DataListItemRow>
                  <DataListToggle
                    onClick={() => this.onToggle(appIndex)}
                    isExpanded={this.state.isRowOpen[appIndex]}
                    id={'applicationToggle' + appIndex}
                    aria-controls="expandable"
                  />
                  <DataListItemCells
                    dataListCells={[
                      <DataListCell width={2} key={'app-' + appIndex}>
                        <BuilderImageIcon /> <Text component={TextVariants.small}>{application.clientId}</Text>
                      </DataListCell>,
                      <DataListCell width={2} key={'internal-' + appIndex}>
                        <span>{application.inUse && !application.userConsentRequired ? 'Internal' : 'Third-party'}
                          {application.offlineAccess ? ', Offline Access' : ''}
                        </span>
                      </DataListCell>,
                      <DataListCell width={2} key={'status-' + appIndex}>
                        <span>{application.inUse ? 'In use' : 'Not in use'}</span>
                      </DataListCell>,
                      <DataListCell width={4} key={'baseUrl-' + appIndex}>
                        <a href={application.baseUrl}><LinkIcon /> {application.baseUrl}</a>
                      </DataListCell>,
                    ]}
                  />
                </DataListItemRow>
                <DataListContent
                  noPadding={false}
                  aria-label="Application Details"
                  id="expandable"
                  isHidden={!this.state.isRowOpen[appIndex]}
                >
                  <Grid sm={12} md={12} lg={12}>
                    <GridItem><b>{'Client: '}</b> {application.clientId}</GridItem>
                    {application.description &&
                      <GridItem><b>{'Description: '}</b> {application.description}</GridItem>
                    }
                    <GridItem><b>{'URL: '}</b> {application.baseUrl}</GridItem>
                    {application.consent &&
                      <React.Fragment>
                        <GridItem span={12}>
                          <b>Has access to:</b>
                        </GridItem>
                        {application.consent.grantedScopes.map((scope: GrantedScope, scopeIndex: number) => {
                          return (
                            <React.Fragment key={'scope-' + scopeIndex} >
                              <GridItem offset={1}><CheckIcon /> {scope.name}</GridItem>
                            </React.Fragment>
                          )
                        })}
                        <GridItem><b>{'Access granted on: '}</b>
                          {new Intl.DateTimeFormat('en-US', {
                            year: 'numeric',
                            month: 'long',
                            day: 'numeric',
                            hour: 'numeric',
                            minute: 'numeric',
                            second: 'numeric'
                          }).format(application.consent.createDate)}
                        </GridItem>
                      </React.Fragment>
                    }
                  </Grid>
                  <Grid gutter='sm'>
                    <hr />
                    <GridItem>
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
                    <GridItem><InfoAltIcon /> By clicking ‘Remove Access’, you will remove granted permissions of this app. This app will no longer use your information.</GridItem>
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
