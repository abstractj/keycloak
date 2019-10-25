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
import {AxiosResponse} from 'axios';

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
} from '@patternfly/react-core';

import {InfoAltIcon, LinkIcon, BuilderImageIcon} from '@patternfly/react-icons';
import {ContentPage} from '../ContentPage'; 
import {ContinueCancelModal} from '../../widgets/ContinueCancelModal';
import {AccountServiceClient} from '../../account-service/account.service';

export interface ApplicationsPageProps {
}

export interface ApplicationsPageState {
  isRowOpen: boolean[];
  applications: Application[];
}

interface Application {
  clientId: string;
  clientName: string;
  internal: boolean;
  inUse: boolean;
  baseUrl: string;
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

  private doSomething = () => {
    //TODO
    //console.log("Test");
  }

  private onToggle = (row: number): void => {
    const newIsRowOpen: boolean[] = this.state.isRowOpen;
    newIsRowOpen[row] = !newIsRowOpen[row];
    this.setState({isRowOpen: newIsRowOpen});
  };

  private handleChange = () => {
    //TODO
  };

  private filterApplications = () => {
    console.log('Search applications');
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
                         <a href={application.baseUrl}><LinkIcon />{application.baseUrl}</a>
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
                  <div className="pf-c-content">
                    <Text component={TextVariants.h3}>Client</Text>
                    <hr/>
                    <Text component={TextVariants.h3}>Description</Text>
                    <Text component={TextVariants.h3}>URL</Text>
                    <Text component={TextVariants.h3}>Has access to</Text>
                    <Text component={TextVariants.h3}>Access granted on</Text>
                    <hr/>
                    <React.Fragment>
                      <ContinueCancelModal 
                        buttonTitle='Remove' // required
                        buttonVariant='secondary' // defaults to 'primary'
                        modalTitle='Remove Access' // required
                        modalMessage='This will remove the currently granted access permission for AdminCLI. You will need to grant access again if you want ot user this app.'
                        modalContinueButtonLabel='Confirm' // defaults to 'Continue'
                        modalCancelButtonLabel='Cancel' // defaults to 'Cancel'
                        onContinue={this.doSomething} // required
                      />
                    </React.Fragment>
                    <span>
                      By clicking ‘Remove Access’, you will remove granted permissions of this app. This app will no longer use your information.
                    </span>
                  </div>
                </DataListContent>
              </DataListItem>
            )
          })}
        </DataList>
      </ContentPage>
    );
  }
};
